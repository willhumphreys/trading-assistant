package uk.co.threebugs.darwinexclient.metatrader

import org.json.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.utils.*
import uk.co.threebugs.darwinexclient.websocket.*
import java.math.*
import java.time.*

/*Custom event handler implementing the EventHandler interface.
*/
@Component
class TradeEventHandler(

    private val webSocketController: WebSocketController,
    private val tradeService: TradeService,
    private val slackClient: SlackClient,
    val accountSetupGroupsMapper: AccountSetupGroupsMapper,
    private val clock: Clock

) {

    //    public void start(Client dwx, String[] symbols) {
    //
    //        // account information is stored in dwx.accountInfo.
    //        logger.info("\nAccount info:\n" + dwx.accountInfo + "\n");
    //
    //        // subscribe to tick data:
    //        dwx.subscribeSymbols(symbols);
    //
    //
    //        // subscribe to bar data:
    ////        var symbolsBarData = new String[][]{{"EURUSD", "M1"}, {"AUDCAD", "M5"}, {"GBPCAD", "M15"}};
    ////        dwx.subscribeSymbolsBarData(symbolsBarData);
    //
    //        // request historic data:
    ////        var end = System.currentTimeMillis() / 1000;
    ////        var start = end - 10 * 24 * 60 * 60;  // last 10 days
    ////        dwx.getHistoricData("AUDCAD", "D1", start, end);
    //
    //        // dwx.closeOrdersByMagic(77);
    //        // sleep(2000);
    //    }
    // use synchronized so that price updates and execution updates are not processed one after the other.
    @Synchronized
    fun onTick(
        dwx: Client,
        symbol: String,
        bid: BigDecimal,
        ask: BigDecimal,
        accountSetupGroupsDto: AccountSetupGroupsDto
    ) {

        webSocketController.sendMessage(
            WebSocketMessage(id = 0, field = "tick", value = "$symbol, $bid, $ask"),
            "/topic/ticks"
        )

        val accountSetupGroups = accountSetupGroupsMapper.toEntity(accountSetupGroupsDto)
        tradeService.createTradesToPlaceFromEnabledSetups(symbol, accountSetupGroups)
        tradeService.placeTrades(dwx, symbol, bid, ask, accountSetupGroups)
        tradeService.closeTradesAtTime(dwx, symbol, accountSetupGroups)
    }


    @Synchronized
    fun onMessage(message: JSONObject) {
        if (message["type"]
            == "ERROR"
        ) logger.info(message["type"].toString() + " | " + message["error_type"] + " | " + message["description"]) else if (message["type"]
            == "INFO"
        ) logger.info(message["type"].toString() + " | " + message["message"])
        slackClient.sendSlackNotification("message: $message")

        webSocketController.sendMessage(WebSocketMessage(id = 0, field = "message", value = "$message"), "/topic/ticks")

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
