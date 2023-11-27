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
import uk.co.threebugs.darwinexclient.metatrader.commands.*
import uk.co.threebugs.darwinexclient.metatrader.marketdata.*
import uk.co.threebugs.darwinexclient.metatrader.messages.*
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
    @param:Value("\${accounts-dir}") private val accounts: String,
    private val setupGroupService: SetupGroupService,
    private val setupRepository: SetupRepository,
    private val setupFileRepository: SetupFileRepository,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val webSocketController: WebSocketController,
    private val actionsService: ActionsService,
    private val messageRepository: MessageRepository,
    private val messageService: MessageService,
    private val marketDataRepository: MarketDataRepository,
    private val marketDataService: MarketDataService,
    private val commandService: CommandService,
    private val objectMapper: ObjectMapper
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
    private final val dwxPath: Path
    private final val pathMap: Map<String, Path>
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

        messageRepository.loadMessages(accountSetupGroupsName)

        thread(name = "openOrdersThread") {

            checkOpenOrders()

        }
        thread(name = "checkMessage") { messageService.checkMessages(accountSetupGroupsName) }
        thread(name = "checkMarketData") { checkMarketData() }

        commandService.resetCommandIDs(accountSetupGroupsName)
        loadOrders()
        logger.info("\nAccount info:\n${openOrders.accountInfo}\n")

        // subscribe to tick data:
        commandService.subscribeSymbols(symbols, accountSetupGroupsName)

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
                    Path.of(setupGroup.path ?: throw RuntimeException("Path not found")),
                    setupGroup.symbol ?: throw RuntimeException("Symbol not found"),
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


    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private fun checkMarketData() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            val data = marketDataRepository.loadMarketData(accountSetupGroupsName)

            data.forEach { (symbol, newCurrencyInfo) ->

                eventHandler.onTick(symbol, newCurrencyInfo.bid, newCurrencyInfo.ask, accountSetupGroupsDto)

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


}
