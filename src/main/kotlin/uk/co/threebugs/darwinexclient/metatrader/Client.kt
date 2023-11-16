package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.core.type.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.exc.*
import com.fasterxml.jackson.module.kotlin.*
import org.json.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.tradingstance.*
import uk.co.threebugs.darwinexclient.utils.*
import uk.co.threebugs.darwinexclient.websocket.*
import java.io.*
import java.math.*
import java.nio.file.*
import java.util.*
import kotlin.concurrent.*


@Component
class Client(
    private val eventHandler: TradeEventHandler,
    private val accountService: AccountService,
    @param:Value("\${account-setup-groups-name}") private val accountSetupGroupsName: String,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,
    @param:Value("\${max-retry-command-seconds}") private val maxRetryCommandSeconds: Int,
    @param:Value("\${accounts-dir}") private val accounts: String,
    private val setupGroupService: SetupGroupService,
    private val setupRepository: SetupRepository,
    private val setupFileRepository: SetupFileRepository,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val webSocketController: WebSocketController,
    private val objectMapper: ObjectMapper,
    private val actionsService: ActionsService
) {

    var openOrders: Orders = Orders(
        accountInfo = AccountInfo(
            number = 0,
            leverage = 0,
            balance = BigDecimal.ZERO,
            freeMargin = BigDecimal.ZERO,
            name = "dummy",
            currency = "",
            equity = BigDecimal.ZERO
        ), orders = mapOf()
    )
    var barData = JSONObject()
    var historicData = JSONObject()
    var historicTrades = JSONObject()
    private final val dwxPath: Path
    private final val pathMap: Map<String, Path>
    private var commandID = 0
    private var lastMessagesMillis: Long = 0
    private var lastOpenOrders: Orders = Orders(
        accountInfo = AccountInfo(
            number = 0,
            leverage = 0,
            balance = BigDecimal.ZERO,
            freeMargin = BigDecimal.ZERO,
            name = "dummy",
            currency = "",
            equity = BigDecimal.ZERO
        ), orders = mapOf()
    )
    private var lastMessagesStr: String? = ""
    private var lastBarDataStr: String? = ""
    private var lastHistoricDataStr: String? = ""
    private var lastHistoricTradesStr: String? = ""
    private var lastBarData = JSONObject()
    private var lastMarketData: Map<String, CurrencyInfo> = java.util.Map.of()

    private final val accountSetupGroupsDto: AccountSetupGroupsDto

    private final val accountsPath: Path = Paths.get(accounts)

    init {
        val accountDtos = loadMetaTraderInstalls(accountsPath.resolve("metatrader_dirs.json"))
        val symbols = arrayOf("EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF")

        val setupGroupsPath = Paths.get(accounts, "setup-groups")
        try {
            Files.list(setupGroupsPath).use { paths ->
                paths.forEach { setupsPath: Path ->
                    val setupGroups = setupGroupService.loadSetupsFromFile(setupsPath)
                    Arrays.stream(symbols)
                        .forEach { symbol: String -> loadDataFromCsv(symbol, setupGroups) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val accounts =
            accountSetupGroupsService.loadAccountSetupGroups(accountsPath.resolve("account-setup-groups.json"))
        accountSetupGroupsDto = accountSetupGroupsService.findByName(accountSetupGroupsName)
            ?: throw RuntimeException("Failed to find account setup groups: $accountSetupGroupsName")


        val metaTraderDirPath = accountSetupGroupsDto.account.metatraderAdvisorPath
        val f = metaTraderDirPath.toFile()
        if (!f.exists()) {
            logger.info("ERROR: MetaTraderDirPath does not exist!")
            throw RuntimeException("ERROR: MetaTraderDirPath does not exist! $metaTraderDirPath")
        }
        dwxPath = metaTraderDirPath.resolve("DWX")

        pathMap = mapOf(
            "pathOrders" to dwxPath.resolve("DWX_Orders.json"),
            "pathMessages" to dwxPath.resolve("DWX_Messages.json"),
            "pathMarketData" to dwxPath.resolve("DWX_Market_Data.json"),
            "pathBarData" to dwxPath.resolve("DWX_Bar_Data.json"),
            "pathHistoricData" to dwxPath.resolve("DWX_Historic_Data.json"),
            "pathHistoricTrades" to dwxPath.resolve("DWX_Historic_Trades.json"),
            "pathOrdersStored" to dwxPath.resolve("DWX_Orders_Stored.json"),
            "pathMessagesStored" to dwxPath.resolve("DWX_Messages_Stored.json"),

            )

        loadMessages()

        thread(name = "openOrdersThread") {

            checkOpenOrders()

        }
        thread(name = "checkMessage") { checkMessages() }
        thread(name = "checkMarketData") { checkMarketData() }
        thread(name = "checkBarData") { checkBarData() }
        thread(name = "checkHistoricData") { checkHistoricData() }

        resetCommandIDs()
        loadOrders()
        logger.info("\nAccount info:\n${openOrders.accountInfo}\n")

        // subscribe to tick data:
        subscribeSymbols(symbols)

        actionsService.startUpComplete()


    }

    private fun loadMetaTraderInstalls(path: Path): List<AccountDto> {
        return try {
            val metaTraderDirs = objectMapper.readValue<List<MetaTraderDir>>(path.toFile())
            metaTraderDirs.map { metaTraderDir ->
                accountService.findByName(metaTraderDir.name)
                    ?.also { logger.info("MetaTraderDir ${metaTraderDir.name} found in database") }
                    ?: run {
                        logger.info("MetaTraderDir ${metaTraderDir.name} not found in database")
                        accountService.save(
                            AccountDto(
                                metatraderAdvisorPath = Path.of(metaTraderDir.dirPath),
                                name = metaTraderDir.name
                            )
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("Error loading MetaTrader directories from JSON file", e)
            throw RuntimeException("Error loading MetaTrader directories from JSON file", e)
        }
    }


    private fun loadDataFromCsv(symbol: String, setupGroups: List<SetupGroup>): List<Setup> {
        return setupGroups
            .filter { setupGroup: SetupGroup ->
                setupGroup.symbol
                    .equals(symbol, ignoreCase = true)
            } //.filter(SetupGroup::getEnabled)
            .flatMap { setupGroup: SetupGroup ->
                setupFileRepository.readCsv(
                    Path.of(setupGroup.path!!),
                    setupGroup.symbol!!,
                    setupGroup
                )
            }
            .map { setup: Setup ->
                val setup1 = setupRepository.findBySymbolAndRankAndSetupGroup(symbol, setup.rank!!, setup.setupGroup!!)
                if (setup1 == null) {
                    setupRepository.save(setup)
                }
                setup
            }

    }

    /*Regularly checks the file for open orders and triggers
    the eventHandler.onOrderEvent() function.
    */
    private fun checkOpenOrders() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            val ordersPath =
                pathMap["pathOrders"] ?: throw NoSuchElementException("Key 'pathOrders' not found")


            if (!ordersPath.toFile().exists()) {
                logger.warn("Orders file does not exist: $ordersPath")
                continue
            }

            try {
                val data: Orders = objectMapper.readValue(ordersPath.toFile())

                //   if (data.orders.isEmpty()) continue

                openOrders = data

                // Get the keys (order IDs) from both maps
                val openOrderIds = openOrders.orders.keys
                val lastOpenOrderIds = lastOpenOrders.orders.keys

                // Find IDs that are in openOrderIds but not in lastOpenOrderIds
                val newOrders = openOrderIds - lastOpenOrderIds

                // Find IDs that are in lastOpenOrderIds but not in openOrderIds
                val closedOrders = lastOpenOrderIds - openOrderIds

                closedOrders.forEach {
                    logger.info("Order removed: $it")
                    lastOpenOrders.orders[it]?.let { it1 -> eventHandler.onClosedOrder(it1) }
                }

                newOrders.forEach {
                    logger.info("Order added: $it")
                    openOrders.orders[it]?.let { it1 -> eventHandler.onNewOrder(it1, it) }
                }

                for ((key, currentOrder) in openOrders.orders) {

                    // Check if the key exists in previousDataOrders
                    if (lastOpenOrders.orders.containsKey(key)) {
                        val previousOrder = lastOpenOrders.orders[key]

                        // Compare the TradeInfo objects
                        compareTradeInfo(key, currentOrder, previousOrder!!)
                    } else {
                        // Log new orders that didn't exist in previousDataOrders
                        logger.info("New order: $key, Value: $currentOrder")
                    }
                }

                lastOpenOrders = data
                Helpers.tryWriteToFile(pathMap["pathOrdersStored"], objectMapper.writeValueAsString(data))
            } catch (e: JsonProcessingException) {
                logger.error("JsonProcessingException checking open orders", e)

            } catch (e1: MismatchedInputException) {
                logger.error("MismatchedInputException checking open orders", e1)
            } catch (e2: FileNotFoundException) {
                logger.error("File not found", e2)
            }
        }
    }

    private fun compareTradeInfo(ticket: Int, currentValue: TradeInfo, previousValue: TradeInfo) {
        var log = false
        if (currentValue != previousValue) {
            val changes = StringBuilder("Changes for Order $ticket: ")
            if (currentValue.magic != previousValue.magic) {
                changes.append("Magic: ")
                    .append(previousValue.magic)
                    .append(" -> ")
                    .append(currentValue.magic)
                    .append(", ")
                log = true
            }
            if (currentValue.lots != previousValue.lots) {
                changes.append("Lots: ")
                    .append(previousValue.lots)
                    .append(" -> ")
                    .append(currentValue.lots)
                    .append(", ")
                log = true
            }
            if (currentValue.symbol != previousValue.symbol) {
                changes.append("Symbol: ")
                    .append(previousValue.symbol)
                    .append(" -> ")
                    .append(currentValue.symbol)
                    .append(", ")
                log = true
            }
            if (currentValue.swap != previousValue.swap) {
                changes.append("Swap: ")
                    .append(previousValue.swap)
                    .append(" -> ")
                    .append(currentValue.swap)
                    .append(", ")
                log = true
            }
            if (currentValue.openTime != previousValue.openTime) {
                changes.append("Open Time: ")
                    .append(previousValue.openTime)
                    .append(" -> ")
                    .append(currentValue.openTime)
                    .append(", ")
                log = true
            }
            if (currentValue.stopLoss != previousValue.stopLoss) {
                changes.append("Stop Loss: ")
                    .append(previousValue.stopLoss)
                    .append(" -> ")
                    .append(currentValue.stopLoss)
                    .append(", ")
                log = true
            }
            if (currentValue.comment != previousValue.comment) {
                changes.append("Comment: ")
                    .append(previousValue.comment)
                    .append(" -> ")
                    .append(currentValue.comment)
                    .append(", ")
                log = true
            }
            if (currentValue.type != previousValue.type) {
                changes.append("Type: ")
                    .append(previousValue.type)
                    .append(" -> ")
                    .append(currentValue.type)
                    .append(", ")

                this.eventHandler.onTradeStateChange(currentValue, previousValue)
                log = true
            }
            if (currentValue.openPrice != previousValue.openPrice) {
                changes.append("Open Price: ")
                    .append(previousValue.openPrice)
                    .append(" -> ")
                    .append(currentValue.openPrice)
                    .append(", ")
                log = true
            }
            if (currentValue.takeProfit != previousValue.takeProfit) {
                changes.append("Take Profit: ")
                    .append(previousValue.takeProfit)
                    .append(" -> ")
                    .append(currentValue.takeProfit)
                    .append(", ")
                log = true
            }
            if (currentValue.profitAndLoss != previousValue.profitAndLoss) {
                changes.append("Profit and Loss: ")
                    .append(previousValue.profitAndLoss)
                    .append(" -> ")
                    .append(currentValue.profitAndLoss)


                webSocketController.sendMessage(
                    WebSocketMessage(
                        id = currentValue.magic,
                        field = "profitAndLoss",
                        value = currentValue.profitAndLoss.toString()
                    ), "/topic/order-change"
                )

                log = true

            }
            if (currentValue.mapType != previousValue.mapType) {
                changes.append("Map Type: ")
                    .append(previousValue.mapType)
                    .append(" -> ")
                    .append(currentValue.mapType)
                    .append(", ")
                log = true
            }
            if (currentValue.empty != previousValue.empty) {
                changes.append("Empty: ")
                    .append(previousValue.empty)
                    .append(" -> ")
                    .append(currentValue.empty)
            }
            if (log) {
//                webSocketController.sendMessage(webSocketMessage(
//                        id = currentValue.magic,
//                        field = "Order",
//                        value = changes.toString()
//                        ), "/topic/order-change")
            }
        }
    }


    /*Regularly checks the file for messages and triggers
    the eventHandler.onMessage() function.
    */
    private fun checkMessages() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue


            val messagesPath =
                pathMap["pathMessages"] ?: throw NoSuchElementException("Key 'pathMessages' not found")
            val text = Helpers.tryReadFile(messagesPath)
            if (text.isEmpty() || text == lastMessagesStr) continue
            lastMessagesStr = text
            val data: JSONObject = try {
                JSONObject(text)
            } catch (e: Exception) {
                continue
            }

            // the objects are not ordered. because of (millis > lastMessagesMillis) it would miss messages if we just looped through them directly.
            val millisList = ArrayList<String>()
            for (millisStr in data.keySet()) {
                if (data[millisStr] != null) {
                    millisList.add(millisStr)
                }
            }
            Collections.sort(millisList)
            for (millisStr in millisList) {
                if (data[millisStr] != null) {
                    val millis = millisStr.toLong()
                    if (millis > lastMessagesMillis) {
                        lastMessagesMillis = millis
                        eventHandler.onMessage(this, data[millisStr] as JSONObject)
                    }
                }
            }
            Helpers.tryWriteToFile(messagesPath, data.toString())
        }
    }

    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private fun checkMarketData() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            val marketDataPath =
                pathMap["pathMarketData"] ?: throw NoSuchElementException("Key 'pathMarketData' not found")

            if (!Files.exists(marketDataPath)) {
                logger.warn("Market data file does not exist: $marketDataPath")
                continue
            }

            val data: Map<String, CurrencyInfo> = runCatching {
                objectMapper.readValue(
                    marketDataPath.toFile(),
                    object : TypeReference<Map<String, CurrencyInfo>>() {})
            }.getOrElse { throwable ->
                logger.error("An error occurred while reading the marketData file. Returning an emptyMap: $throwable")
                emptyMap()
            }

            if (data.isEmpty()) continue

            data.forEach { (symbol, newCurrencyInfo) ->
                val lastCurrencyInfo = lastMarketData[symbol]

                if (lastCurrencyInfo == null || newCurrencyInfo != lastCurrencyInfo) {
                    eventHandler.onTick(this, symbol, newCurrencyInfo.bid, newCurrencyInfo.ask, accountSetupGroupsDto)
                }
            }


            lastMarketData = data
        }
    }

    /*Regularly checks the file for bar data and triggers
    the eventHandler.onBarData() function.
    */
    private fun checkBarData() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            val text = Helpers.tryReadFile(
                pathMap["pathBarData"] ?: throw NoSuchElementException("Key 'pathBarData' not found")
            )

            if (text.isEmpty() || text == lastBarDataStr) continue
            lastBarDataStr = text
            var data: JSONObject
            data = try {
                JSONObject(text)
            } catch (e: Exception) {
                continue
            }
            barData = data

            for (st in barData.keySet()) {
                if (!lastBarData.has(st) || barData[st] != lastBarData[st]) {
                    val stSplit = st.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (stSplit.size != 2) continue
                    val jo = barData[st] as JSONObject
                    eventHandler.onBarData(
                        this,
                        stSplit[0],
                        stSplit[1],
                        jo["time"] as String,
                        jo["open"] as BigDecimal,
                        jo["high"] as BigDecimal,
                        jo["low"] as BigDecimal,
                        jo["close"] as BigDecimal,
                        jo["tick_volume"] as Int
                    )
                }
            }

            lastBarData = data
        }
    }

    /*Regularly checks the file for historic data and triggers
    the eventHandler.onHistoricData() function.
    */
    private fun checkHistoricData() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning()) {
                continue
            }
            val historicDataPath =
                pathMap["pathHistoricData"] ?: throw NoSuchElementException("Key 'pathHistoricData' not found")
            val text = Helpers.tryReadFile(historicDataPath)
            if (!text.isEmpty() && text != lastHistoricDataStr!!) {
                lastHistoricDataStr = text
                var data: JSONObject?
                data = try {
                    JSONObject(text)
                } catch (e: Exception) {
                    null
                }
                if (data != null) {
                    for (st in data.keySet()) {
                        historicData.put(st, data[st])
                    }
                    Helpers.tryDeleteFile(historicDataPath)

                    for (st in data.keySet()) {
                        val stSplit = st.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (stSplit.size != 2) continue
                        eventHandler.onHistoricData(this, stSplit[0], stSplit[1], data[st] as JSONObject)
                    }

                }
            }

            // also check historic trades in the same thread.
            val historicTradesText = Helpers.tryReadFile(
                pathMap["pathHistoricTrades"] ?: throw NoSuchElementException("Key 'pathHistoricTrades' not found")
            )

            if (!historicTradesText.isEmpty() && historicTradesText != lastHistoricTradesStr) {
                lastHistoricTradesStr = historicTradesText
                var data: JSONObject?
                data = try {
                    JSONObject(historicTradesText)
                } catch (e: Exception) {
                    null
                }
                if (data != null) {
                    historicTrades = data
                    eventHandler.onHistoricTrades(this)
                }
            }
        }
    }

    /*Loads stored orders from file (in case of a restart).
     */
    @Throws(JsonProcessingException::class)
    private fun loadOrders() {
        val storedOrdersPath =
            pathMap["pathOrdersStored"] ?: throw NoSuchElementException("Key 'pathOrdersStored' not found")

        if (!storedOrdersPath.toFile().exists()) {
            logger.warn("No stored orders found")
            return
        }

        try {
            val storedOrders = objectMapper.readValue<Orders>(storedOrdersPath.toFile())
            lastOpenOrders = storedOrders
            openOrders = storedOrders
        } catch (e: Exception) {
            logger.error("Error loading stored orders", e)
            val tryReadFile = Helpers.tryReadFile(storedOrdersPath)
            logger.info("Stored orders: $tryReadFile")
        }

    }

    /*Loads stored messages from file (in case of a restart).
     */
    private fun loadMessages() {
        val text = Helpers.tryReadFile(
            pathMap["pathMessagesStored"] ?: throw NoSuchElementException("Key 'pathMessagesStored' not found")
        )
        if (text.isEmpty()) return
        val data: JSONObject
        data = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        lastMessagesStr = text

        // here we don't have to sort because we just need the latest millis value.
        for (millisStr in data.keySet()) {
            if (data.has(millisStr)) {
                val millis = millisStr.toLong()
                if (millis > lastMessagesMillis) lastMessagesMillis = millis
            }
        }
    }

    /*Sends a SUBSCRIBE_SYMBOLS command to subscribe to market (tick) data.

    Args:
        symbols (String[]): List of symbols to subscribe to.

    Returns:
        null

        The data will be stored in marketData.
        On receiving the data the eventHandler.onTick()
        function will be triggered.
    */
    final fun subscribeSymbols(symbols: Array<String>) {
        sendCommand("SUBSCRIBE_SYMBOLS", java.lang.String.join(",", *symbols))
    }

    /*Sends a SUBSCRIBE_SYMBOLS_BAR_DATA command to subscribe to bar data.

    Args:
        symbols (String[][]): List of lists containing symbol/time frame
        combinations to subscribe to. For example:
        String[][] symbols = {{"EURUSD", "M1"}, {"USDJPY", "H1"}};

    Returns:
        null

        The data will be stored in barData.
        On receiving the data the eventHandler.onBarData()
        function will be triggered.
    */
    fun subscribeSymbolsBarData(symbols: Array<Array<String?>>) {
        val content = StringBuilder()
        for (i in symbols.indices) {
            if (i != 0) content.append(",")
            content.append(symbols[i][0])
                .append(",")
                .append(symbols[i][1])
        }
        sendCommand("SUBSCRIBE_SYMBOLS_BAR_DATA", content.toString())
    }

    /*Sends a GET_HISTORIC_DATA command to request historic data.

    Args:
        symbol (String): Symbol to get historic data.
        timeFrame (String): Time frame for the requested data.
        start (long): Start timestamp (seconds since epoch) of the requested data.
        end (long): End timestamp of the requested data.

    Returns:
        null

        The data will be stored in historicData.
        On receiving the data the eventHandler.onHistoricData()
        function will be triggered.
    */
    fun getHistoricData(symbol: String, timeFrame: String, start: Long, end: Long) {
        val content = "$symbol,$timeFrame,$start,$end"
        sendCommand("GET_HISTORIC_DATA", content)
    }

    /*Sends a GET_HISTORIC_TRADES command to request historic trades.

    Kwargs:
        lookbackDays (int): Days to look back into the trade history.
                            The history must also be visible in MT4.

    Returns:
        None

        The data will be stored in historicTrades.
        On receiving the data the eventHandler.onHistoricTrades()
        function will be triggered.
    */
    fun getHistoricTrades(lookbackDays: Int) {
        sendCommand("GET_HISTORIC_TRADES", lookbackDays.toString())
    }

    /*Sends an OPEN_ORDER command to open an order.

    Args:
        symbol (String): Symbol for which an order should be opened.
        order_type (String): Order type. Can be one of:
            'buy', 'sell', 'buylimit', 'selllimit', 'buystop', 'sellstop'
        lots (double): Volume in lots
        price (double): Price of the (pending) order. Can be zero
            for market orders.
        stop_loss (double): SL as absoute price. Can be zero
            if the order should not have an SL.
        take_profit (double): TP as absoute price. Can be zero
            if the order should not have a TP.
        magic (int): Magic number
        comment (String): Order comment
        expriation (long): Expiration time given as timestamp in seconds.
            Can be zero if the order should not have an expiration time.
    */
    fun openOrder(order: Order) {
        logger.info("openOrder: " + order.symbol + ", " + order.orderType + ", " + order.lots + ", " + order.price + ", " + order.stopLoss + ", " + order.takeProfit + ", " + order.magic + ", " + order.comment + ", " + order.expiration)
        val content =
            order.symbol + "," + order.orderType + "," + order.lots + "," + order.price + "," + order.stopLoss + "," + order.takeProfit + "," + order.magic + "," + order.comment + "," + order.expiration
        sendCommand("OPEN_ORDER", content)
        logger.info("order sent: $content")
    }

    /*Sends a MODIFY_ORDER command to modify an order.

    Args:
        ticket (int): Ticket of the order that should be modified.
        lots (double): Volume in lots
        price (double): Price of the (pending) order. Non-zero only
            works for pending orders.
        stop_loss (double): New stop loss price.
        take_profit (double): New take profit price.
        expriation (long): New expiration time given as timestamp in seconds.
            Can be zero if the order should not have an expiration time.
    */
    fun modifyOrder(
        ticket: Int,
        lots: Double,
        price: Double,
        stopLoss: Double,
        takeProfit: Double,
        expiration: Long
    ) {
        val content = "$ticket,$lots,$price,$stopLoss,$takeProfit,$expiration"
        sendCommand("MODIFY_ORDER", content)
    }

    /*Sends a CLOSE_ORDER command with lots=0 to close an order completely.
     */
    fun closeOrder(ticket: Int) {
        val content = "$ticket,0"
        sendCommand("CLOSE_ORDER", content)
    }

    /*Sends a CLOSE_ORDER command to close an order.

    Args:
        ticket (int): Ticket of the order that should be closed.
        lots (double): Volume in lots. If lots=0 it will try to
            close the complete position.
    */
    fun closeOrder(ticket: Int, lots: Double) {
        val content = "$ticket,$lots"
        sendCommand("CLOSE_ORDER", content)
    }

    /*Sends a CLOSE_ALL_ORDERS command to close all orders.
     */
    fun closeAllOrders() {
        sendCommand("CLOSE_ALL_ORDERS", "")
    }

    /*Sends a CLOSE_ORDERS_BY_SYMBOL command to close all orders
    with a given symbol.

    Args:
        symbol (str): Symbol for which all orders should be closed.
    */
    fun closeOrdersBySymbol(symbol: String) {
        sendCommand("CLOSE_ORDERS_BY_SYMBOL", symbol)
    }

    /*Sends a CLOSE_ORDERS_BY_MAGIC command to close all orders
    with a given magic number.

    Args:
        magic (str): Magic number for which all orders should
            be closed.
    */
    fun closeOrdersByMagic(magic: Int) {
        sendCommand("CLOSE_ORDERS_BY_MAGIC", magic.toString())
    }

    /*Sends a RESET_COMMAND_IDS command to reset stored command IDs.
    This should be used when restarting the java side without restarting
    the mql side.
    */
    final fun resetCommandIDs() {
        commandID = 0
        sendCommand("RESET_COMMAND_IDS", "")

        // sleep to make sure it is read before other commands.
        Helpers.sleep(500)
    }

    /*Sends a command to the mql server by writing it to
    one of the command files.

    Multiple command files are used to allow for fast execution
    of multiple commands in the correct chronological order.

    The method needs to be synchronized so that different threads
    do not use the same commandID or write at the same time.
    */
    @Synchronized
    fun sendCommand(command: String, content: String) {
        commandID = (commandID + 1) % 100000
        val text = "<:$commandID|$command|$content:>"
        var now = System.currentTimeMillis()
        val endMillis = now + maxRetryCommandSeconds * 1000L

        // trying again for X seconds in case all files exist or are
        // currently read from mql side.
        while (now < endMillis) {

            // using 10 different files to increase the execution speed
            // for multiple commands.
            var success = false
            val maxCommandFiles = 20
            for (i in 0 until maxCommandFiles) {

                val filePath = dwxPath.resolve("DWX_Commands_$i.txt")
                if (!filePath.toFile().exists() && Helpers.tryWriteToFile(filePath, text)) {
                    success = true
                    break
                }
            }
            if (success) break
            Helpers.sleep(sleepDelay)
            now = System.currentTimeMillis()
        }
    }
}
