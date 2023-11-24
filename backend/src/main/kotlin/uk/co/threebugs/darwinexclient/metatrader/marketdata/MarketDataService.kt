package uk.co.threebugs.darwinexclient.metatrader.marketdata

import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.*

@Service
class MarketDataService(
    private val marketDataRepository: MarketDataRepository,
    private val actionsService: ActionsService,
    private val eventHandler: TradeEventHandler,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int
) {


}