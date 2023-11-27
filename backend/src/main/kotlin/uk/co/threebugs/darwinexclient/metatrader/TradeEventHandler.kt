package uk.co.threebugs.darwinexclient.metatrader

import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.websocket.*
import java.math.*

/*Custom event handler implementing the EventHandler interface.
*/
@Component
class TradeEventHandler(

    private val webSocketController: WebSocketController,
    private val tradeService: TradeService,
    val accountSetupGroupsMapper: AccountSetupGroupsMapper

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
        tradeService.placeTrades(symbol, bid, ask, accountSetupGroups)
        tradeService.closeTrades(symbol, accountSetupGroups)
    }






}
