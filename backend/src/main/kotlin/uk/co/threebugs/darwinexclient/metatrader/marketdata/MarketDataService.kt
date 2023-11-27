package uk.co.threebugs.darwinexclient.metatrader.marketdata

import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.websocket.*
import java.math.*

/*Custom event handler implementing the EventHandler interface.
*/
@Service
class MarketDataService(

    private val webSocketController: WebSocketController,
    private val tradeService: TradeService,
    val accountSetupGroupsMapper: AccountSetupGroupsMapper

) {

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
