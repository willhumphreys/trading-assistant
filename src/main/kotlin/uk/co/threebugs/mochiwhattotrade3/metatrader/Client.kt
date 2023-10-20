package uk.co.threebugs.mochiwhattotrade3.metatrader

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.annotation.PostConstruct
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.co.threebugs.mochiwhattotrade3.MetaTraderDir
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto
import uk.co.threebugs.mochiwhattotrade3.account.AccountService
import uk.co.threebugs.mochiwhattotrade3.accountsetupgroups.AccountSetupGroupsService
import uk.co.threebugs.mochiwhattotrade3.setup.Setup
import uk.co.threebugs.mochiwhattotrade3.setup.SetupFileRepository
import uk.co.threebugs.mochiwhattotrade3.setup.SetupRepository
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroupService
import uk.co.threebugs.mochiwhattotrade3.utils.logger
import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Component
class Client(private val eventHandler: TradeEventHandler, private val accountService: AccountService, @param:Value("\${metatrader-name}") private val metaTraderName: String, @param:Value("\${sleep-delay}") private val sleepDelay: Int, @param:Value("\${max-retry-command-seconds}") private val maxRetryCommandSeconds: Int, private val setupGroupService: SetupGroupService, private val setupRepository: SetupRepository, private val setupFileRepository: SetupFileRepository, private val accountSetupGroupsService: AccountSetupGroupsService, private val objectMapper: ObjectMapper) {
    var openOrders: Map<Int, TradeInfo> = java.util.Map.of()
    var accountInfo = JSONObject()
    var marketData = JSONObject()
    var barData = JSONObject()
    var historicData = JSONObject()
    var historicTrades = JSONObject()
    var ACTIVE = true
    private var pathOrders: Path? = null
    private var pathMessages: Path? = null
    private var pathMarketData: Path? = null
    private var pathBarData: Path? = null
    private var pathHistoricData: Path? = null
    private var pathHistoricTrades: Path? = null
    private var pathOrdersStored: Path? = null
    private var pathMessagesStored: Path? = null
    private var pathCommandsPrefix: Path? = null
    private var commandID = 0
    private var lastMessagesMillis: Long = 0
    private var lastOpenOrdersStr: String? = ""
    private var lastMessagesStr: String? = ""
    private var lastMarketDataStr: String? = ""
    private var lastBarDataStr: String? = ""
    private var lastHistoricDataStr: String? = ""
    private var lastHistoricTradesStr: String? = ""
    private var lastBarData = JSONObject()
    private var lastMarketData = JSONObject()
    private var START = false
    private var account: AccountDto? = null

    init {
        val mapper = ObjectMapper().registerModule(KotlinModule())

    }

    private fun loadMetaTraderInstalls(path: Path): List<AccountDto> {
        try {
            val metaTraderDirs = objectMapper.readValue(path.toFile(), object : TypeReference<List<MetaTraderDir>>() {})

            return metaTraderDirs.map { metaTraderDir ->
                val foundAccount = accountService.findByName(metaTraderDir.name!!)

                if (foundAccount == null) {
                    logger.info("MetaTraderDir {} not found in database", metaTraderDir.name)
                    accountService.save(AccountDto(
                            metatraderAdvisorPath = Path.of(metaTraderDir.dirPath!!),
                            name = metaTraderDir.name
                    ))
                } else {
                    foundAccount
                }
            }
        } catch (e: Exception) {
            logger.error("Error loading MetaTrader directories from JSON file", e)
            throw RuntimeException("Error loading MetaTrader directories from JSON file", e)
        }
    }

    @PostConstruct
    @Throws(JsonProcessingException::class)
    fun init() {
        objectMapper.registerModule(JavaTimeModule())
        val accountDtos = loadMetaTraderInstalls(Paths.get("accounts", "metatrader_dirs.json"))
        val setupGroupsPath = Paths.get("accounts", "setup-groups")
        val symbols = arrayOf("EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF")
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
        val accounts = accountSetupGroupsService.loadAccountSetupGroups(Paths.get("accounts", "account-setup-groups.json"))
        val accountSetupGroupsDto = accountSetupGroupsService.findByName(metaTraderName)
                ?: throw RuntimeException("Failed to find account setup groups: $metaTraderName")

        account = accountSetupGroupsDto.account!!
        val metaTraderDirPath = account!!.metatraderAdvisorPath
        val f = metaTraderDirPath!!.toFile()
        if (!f.exists()) {
            logger.info("ERROR: MetaTraderDirPath does not exist!")
            System.exit(1)
        }
        val dwxPath = metaTraderDirPath.resolve("DWX")
        pathOrders = dwxPath.resolve("DWX_Orders.txt")
        pathMessages = dwxPath.resolve("DWX_Messages.txt")
        pathMarketData = dwxPath.resolve("DWX_Market_Data.txt")
        pathBarData = dwxPath.resolve("DWX_Bar_Data.txt")
        pathHistoricData = dwxPath.resolve("DWX_Historic_Data.txt")
        pathHistoricTrades = dwxPath.resolve("DWX_Historic_Trades.txt")
        pathOrdersStored = dwxPath.resolve("DWX_Orders_Stored.txt")
        pathMessagesStored = dwxPath.resolve("DWX_Messages_Stored.txt")
        pathCommandsPrefix = dwxPath.resolve("DWX_Commands_")
        loadMessages()
        val openOrdersThread = Thread {
            try {
                checkOpenOrders()
            } catch (e: JsonProcessingException) {
                throw RuntimeException(e)
            }
        }
        openOrdersThread.start()
        val messageThread = Thread { checkMessages() }
        messageThread.start()
        val marketDataThread = Thread { checkMarketData() }
        marketDataThread.start()
        val barDataThread = Thread { checkBarData() }
        barDataThread.start()
        val historicDataThread = Thread { checkHistoricData() }
        historicDataThread.start()
        resetCommandIDs()
        loadOrders()
        logger.info("\nAccount info:\n$accountInfo\n")

        // subscribe to tick data:
        subscribeSymbols(symbols)
        START = true
    }

    private fun loadDataFromCsv(symbol: String, setupGroups: List<SetupGroup>): List<Setup> {
        return setupGroups
                .filter { setupGroup: SetupGroup ->
                    setupGroup.symbol
                            .equals(symbol, ignoreCase = true)
                } //.filter(SetupGroup::getEnabled)
                .flatMap { setupGroup: SetupGroup -> setupFileRepository.readCsv(Path.of(setupGroup.path!!), setupGroup.symbol!!, setupGroup) }
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
    @Throws(JsonProcessingException::class)
    private fun checkOpenOrders() {
        while (ACTIVE) {
            Helpers.sleep(sleepDelay)
            if (!START) continue
            val newDataStr = Helpers.tryReadFile(pathOrders!!)
            if (newDataStr.isEmpty() || newDataStr == lastOpenOrdersStr) continue
            var previousDataOrders = java.util.Map.of<Int?, TradeInfo?>()
            if (lastOpenOrdersStr != null && !lastOpenOrdersStr!!.isEmpty()) {
                val previousDataJSON = JSONObject(lastOpenOrdersStr)
                previousDataOrders = objectMapper.readValue(previousDataJSON.getJSONObject("orders")
                        .toString(), object : TypeReference<Map<Int?, TradeInfo?>?>() {})
            }
            lastOpenOrdersStr = newDataStr
            val newDataJSON = JSONObject(newDataStr)
            val dataOrders: Map<Int, TradeInfo> = objectMapper.readValue(newDataJSON.getJSONObject("orders").toString(), object : TypeReference<Map<Int, TradeInfo>>() {})

            for (ticket in openOrders.keys) {
                if (!dataOrders.containsKey(ticket)) {
                    logger.info("Order removed: " + openOrders[ticket])
                    val tradeInfo = objectMapper.convertValue(openOrders[ticket], TradeInfo::class.java)
                    eventHandler.onClosedOrder(tradeInfo)
                }
            }
            for (ticket in dataOrders.keys) {
                if (!openOrders.containsKey(ticket)) {
                    val tradeInfo = dataOrders[ticket]
                    logger.info("New order: $tradeInfo")
                    eventHandler.onNewOrder(tradeInfo!!, ticket)
                }
            }
            for ((key, currentValue) in dataOrders) {

                // Check if the key exists in previousDataOrders
                if (previousDataOrders.containsKey(key)) {
                    val previousValue = previousDataOrders[key]

                    // Compare the TradeInfo objects
                    compareTradeInfo(key, currentValue, previousValue!!)
                } else {
                    // Log new orders that didn't exist in previousDataOrders
                    logger.info("New order: $key, Value: $currentValue")
                }
            }
            openOrders = dataOrders
            accountInfo = newDataJSON["account_info"] as JSONObject

            //if (loadOrdersFromFile) Helpers.tryWriteToFile(pathOrdersStored, data.toString());

            //if (newEvent) eventHandler.onOrderEvent(this.openOrders);
        }
    }

    private fun compareTradeInfo(ticket: Int, currentValue: TradeInfo, previousValue: TradeInfo) {
        // Compare fields in TradeInfo and log differences
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
            //            if (!currentValue.getProfitAndLoss()
//                             .equals(previousValue.getProfitAndLoss())) {
//                changes.append("Profit and Loss: ")
//                       .append(previousValue.getProfitAndLoss())
//                       .append(" -> ")
//                       .append(currentValue.getProfitAndLoss())
//
//            }
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
                logger.info(changes)
            }
        }
    }

    /*Regularly checks the file for messages and triggers
    the eventHandler.onMessage() function.
    */
    private fun checkMessages() {
        while (ACTIVE) {
            Helpers.sleep(sleepDelay)
            if (!START) continue
            val text = Helpers.tryReadFile(pathMessages!!)
            if (text.isEmpty() || text == lastMessagesStr) continue
            lastMessagesStr = text
            var data: JSONObject
            data = try {
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
            Helpers.tryWriteToFile(pathMessagesStored, data.toString())
        }
    }

    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private fun checkMarketData() {
        while (ACTIVE) {
            Helpers.sleep(sleepDelay)
            if (!START) continue
            val text = Helpers.tryReadFile(pathMarketData!!)
            if (text.isEmpty() || text == lastMarketDataStr) continue
            lastMarketDataStr = text
            var data: JSONObject
            data = try {
                JSONObject(text)
            } catch (e: Exception) {
                continue
            }
            marketData = data
            for (symbol in marketData.keySet()) {
                if (!lastMarketData.has(symbol) || marketData[symbol] != lastMarketData[symbol]) {
                    val jo = marketData[symbol] as JSONObject
                    eventHandler.onTick(this, symbol, jo["bid"] as BigDecimal, jo["ask"] as BigDecimal, account!!)
                }
            }

            lastMarketData = data
        }
    }

    /*Regularly checks the file for bar data and triggers
    the eventHandler.onBarData() function.
    */
    private fun checkBarData() {
        while (ACTIVE) {
            Helpers.sleep(sleepDelay)
            if (!START) continue
            val text = Helpers.tryReadFile(pathBarData!!)
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
                    eventHandler.onBarData(this, stSplit[0], stSplit[1], jo["time"] as String, jo["open"] as BigDecimal, jo["high"] as BigDecimal, jo["low"] as BigDecimal, jo["close"] as BigDecimal, jo["tick_volume"] as Int)
                }
            }

            lastBarData = data
        }
    }

    /*Regularly checks the file for historic data and triggers
    the eventHandler.onHistoricData() function.
    */
    private fun checkHistoricData() {
        while (ACTIVE) {
            Helpers.sleep(sleepDelay)
            if (!START) continue
            var text = Helpers.tryReadFile(pathHistoricData!!)
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
                    Helpers.tryDeleteFile(pathHistoricData)

                    for (st in data.keySet()) {
                        val stSplit = st.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (stSplit.size != 2) continue
                        eventHandler.onHistoricData(this, stSplit[0], stSplit[1], data[st] as JSONObject)
                    }

                }
            }

            // also check historic trades in the same thread.
            text = Helpers.tryReadFile(pathHistoricTrades!!)
            if (!text.isEmpty() && text != lastHistoricTradesStr) {
                lastHistoricTradesStr = text
                var data: JSONObject?
                data = try {
                    JSONObject(text)
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
        val text = Helpers.tryReadFile(pathOrdersStored!!)
        if (text.isEmpty()) return
        val data: JSONObject
        data = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        lastOpenOrdersStr = text
        openOrders = objectMapper.readValue(data.getJSONObject("orders").toString(), object : TypeReference<Map<Int, TradeInfo>>() {})

    }

    /*Loads stored messages from file (in case of a restart).
     */
    private fun loadMessages() {
        val text = Helpers.tryReadFile(pathMessagesStored!!)
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
    fun subscribeSymbols(symbols: Array<String>) {
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
        val content = order.symbol + "," + order.orderType + "," + order.lots + "," + order.price + "," + order.stopLoss + "," + order.takeProfit + "," + order.magic + "," + order.comment + "," + order.expiration
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
    fun modifyOrder(ticket: Int, lots: Double, price: Double, stopLoss: Double, takeProfit: Double, expiration: Long) {
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
    fun resetCommandIDs() {
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
                val filePath = Paths.get(pathCommandsPrefix.toString() + i + ".txt")
                val f = filePath.toFile()
                if (!f.exists() && Helpers.tryWriteToFile(filePath, text)) {
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
