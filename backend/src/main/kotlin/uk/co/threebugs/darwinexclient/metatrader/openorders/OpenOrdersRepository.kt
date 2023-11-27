package uk.co.threebugs.darwinexclient.metatrader.openorders

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.exc.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.*
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.utils.*
import uk.co.threebugs.darwinexclient.websocket.*
import java.io.*
import java.math.*
import java.time.*

@Repository
class OpenOrdersRepository(
    private val objectMapper: ObjectMapper,
    private val webSocketController: WebSocketController,
    private val tradeService: TradeService,
    private val clock: Clock
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


    /*Regularly checks the file for open orders and triggers
the eventHandler.onOrderEvent() function.
*/
    fun checkOpenOrders(accountSetupGroups: AccountSetupGroupsDto) {

        val dwxPath = accountSetupGroups.account.metatraderAdvisorPath.resolve("DWX")

        val ordersPath =
            dwxPath.resolve("DWX_Orders.json") ?: throw NoSuchElementException("Key 'pathOrders' not found")

        if (!ordersPath.toFile().exists()) {
            logger.warn("Orders file does not exist: $ordersPath")
            return
        }

        val storedOrdersPath = dwxPath.resolve("DWX_Orders_Stored.json")
            ?: throw NoSuchElementException("Key 'pathOrdersStored' not found")

        if (!storedOrdersPath.toFile().exists()) {
            logger.warn("Stored orders path not found")
            return
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
                lastOpenOrders.orders[it]?.let { it1 -> onClosedOrder(it1) }
            }

            newOrders.forEach {
                logger.info("Order added: $it")
                openOrders.orders[it]?.let { it1 -> onNewOrder(it1, it) }
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
            Helpers.tryWriteToFile(storedOrdersPath, objectMapper.writeValueAsString(data))
        } catch (e: JsonProcessingException) {
            logger.error("JsonProcessingException checking open orders", e)

        } catch (e1: MismatchedInputException) {
            logger.error("MismatchedInputException checking open orders", e1)
        } catch (e2: FileNotFoundException) {
            logger.error("File not found", e2)
        }

    }

    /*Loads stored orders from file (in case of a restart).
   */
    @Throws(JsonProcessingException::class)
    internal fun loadOrders(accountSetupGroupsDto: AccountSetupGroupsDto) {

        val storedOrdersPath =
            accountSetupGroupsDto.account.metatraderAdvisorPath.resolve("DWX")
                .resolve("DWX_Messages.json")

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

        logger.info("\nAccount info:\n${openOrders.accountInfo}\n")

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

                onTradeStateChange(currentValue, previousValue)
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

    fun onNewOrder(tradeInfo: TradeInfo, metaTraderId: Int) {
        webSocketController.sendMessage(
            WebSocketMessage(
                id = tradeInfo.magic,
                field = "trade",
                value = tradeInfo.toString()
            ), "/topic/order-change"
        )
        tradeService.fillTrade(tradeInfo, metaTraderId)
    }

    fun onClosedOrder(tradeInfo: TradeInfo) {
        webSocketController.sendMessage(
            WebSocketMessage(
                id = tradeInfo.magic,
                field = "trade",
                value = tradeInfo.toString()
            ), "/topic/order-change"
        )
        tradeService.closeTrade(tradeInfo)
    }

    fun onTradeStateChange(currentValue: TradeInfo, previousValue: TradeInfo) {

        if ((previousValue.type.equals("buylimit") && currentValue.type.equals("buy")) ||
            (previousValue.type.equals("selllimit") && currentValue.type.equals("sell"))
        ) {
            tradeService.findById(currentValue.magic)?.let { trade ->
                trade.apply {
                    status = Status.FILLED
                    filledDateTime = ZonedDateTime.now(clock)
                }.also { tradeService.save(it) }
            }
        }
    }


}