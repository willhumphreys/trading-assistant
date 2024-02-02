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
    private val marketDataRepository: MarketDataRepository
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

        tradeService.createTradesToPlaceFromEnabledSetups(symbol, accountSetupGroupsDto)
        tradeService.placeTrades(symbol, bid, ask, accountSetupGroupsDto)
        tradeService.closeTrades(symbol, accountSetupGroupsDto)
    }

    fun processUpdates(accountSetupGroupsDto: AccountSetupGroupsDto) {
        val updates = marketDataRepository.getMarketDataUpdates(accountSetupGroupsDto)
        updates.forEach { (symbol, newCurrencyInfo) ->
            onTick(symbol, newCurrencyInfo.bid, newCurrencyInfo.ask, accountSetupGroupsDto)
        }
    }
}
