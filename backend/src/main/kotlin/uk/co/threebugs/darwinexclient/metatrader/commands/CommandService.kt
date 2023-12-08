package uk.co.threebugs.darwinexclient.metatrader.commands

import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.metatrader.*
import uk.co.threebugs.darwinexclient.utils.*

@Service
class CommandService(
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,
    @param:Value("\${max-retry-command-seconds}") private val maxRetryCommandSeconds: Int,
) {

    private var commandID = 0


    /*Sends a command to the mql server by writing it to
one of the command files.

Multiple command files are used to allow for fast execution
of multiple commands in the correct chronological order.

The method needs to be synchronized so that different threads
do not use the same commandID or write at the same time.
*/
    @Synchronized
    fun sendCommand(command: String, content: String, accountSetupGroupsDto: AccountSetupGroupsDto) {

        val dwxPath = accountSetupGroupsDto.account.metatraderAdvisorPath.resolve("DWX")

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
                    logger.info("Command sent: $text")
                    success = true
                    break
                }
            }
            if (success) break
            Helpers.sleep(sleepDelay)
            now = System.currentTimeMillis()
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
    final fun subscribeSymbols(symbols: List<String>, accountSetupGroupsName: AccountSetupGroupsDto) {
        sendCommand("SUBSCRIBE_SYMBOLS", java.lang.String.join(",", symbols), accountSetupGroupsName)
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
    fun openOrder(order: Order, accountSetupGroupsName: AccountSetupGroupsDto) {
        logger.info("openOrder: " + order.symbol + ", " + order.orderType + ", " + order.lots + ", " + order.price + ", " + order.stopLoss + ", " + order.takeProfit + ", " + order.magic + ", " + order.comment + ", " + order.expiration)
        val content =
            order.symbol + "," + order.orderType + "," + order.lots + "," + order.price + "," + order.stopLoss + "," + order.takeProfit + "," + order.magic + "," + order.comment + "," + order.expiration
        sendCommand("OPEN_ORDER", content, accountSetupGroupsName)
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
        expiration: Long,
        accountSetupGroupsDto: AccountSetupGroupsDto
    ) {
        val content = "$ticket,$lots,$price,$stopLoss,$takeProfit,$expiration"
        sendCommand("MODIFY_ORDER", content, accountSetupGroupsDto)
    }

    /*Sends a CLOSE_ORDER command with lots=0 to close an order completely.
     */
    fun closeOrder(ticket: Int, accountSetupGroupsDto: AccountSetupGroupsDto) {
        val content = "$ticket,0"
        sendCommand("CLOSE_ORDER", content, accountSetupGroupsDto)
    }

    /*Sends a CLOSE_ORDER command to close an order.

    Args:
        ticket (int): Ticket of the order that should be closed.
        lots (double): Volume in lots. If lots=0 it will try to
            close the complete position.
    */
    fun closeOrder(ticket: Int, lots: Double, accountSetupGroupsDto: AccountSetupGroupsDto) {
        val content = "$ticket,$lots"
        sendCommand("CLOSE_ORDER", content, accountSetupGroupsDto)
    }

    /*Sends a CLOSE_ALL_ORDERS command to close all orders.
     */
    fun closeAllOrders(accountSetupGroupsDto: AccountSetupGroupsDto) {
        sendCommand("CLOSE_ALL_ORDERS", "", accountSetupGroupsDto)
    }

    /*Sends a CLOSE_ORDERS_BY_SYMBOL command to close all orders
    with a given symbol.

    Args:
        symbol (str): Symbol for which all orders should be closed.
    */
    fun closeOrdersBySymbol(symbol: String, accountSetupGroupsDto: AccountSetupGroupsDto) {
        sendCommand("CLOSE_ORDERS_BY_SYMBOL", symbol, accountSetupGroupsDto)
    }

    /*Sends a CLOSE_ORDERS_BY_MAGIC command to close all orders
    with a given magic number.

    Args:
        magic (str): Magic number for which all orders should
            be closed.
    */
    fun closeOrdersByMagic(magic: Int, accountSetupGroups: AccountSetupGroupsDto) {
        sendCommand("CLOSE_ORDERS_BY_MAGIC", magic.toString(), accountSetupGroups)
    }

    /*Sends a RESET_COMMAND_IDS command to reset stored command IDs.
    This should be used when restarting the java side without restarting
    the mql side.
    */
    final fun resetCommandIDs(accountSetupGroupsDto: AccountSetupGroupsDto) {
        commandID = 0
        sendCommand("RESET_COMMAND_IDS", "", accountSetupGroupsDto)

        // sleep to make sure it is read before other commands.
        Helpers.sleep(500)
    }

}