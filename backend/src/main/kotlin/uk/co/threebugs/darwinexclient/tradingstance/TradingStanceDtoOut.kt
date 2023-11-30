package uk.co.threebugs.darwinexclient.tradingstance

import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.trade.*

data class TradingStanceDtoOut(
    val id: Int? = null,
    val symbol: String,
    val direction: Direction,
    val accountSetupGroups: AccountSetupGroupsDto,
    val trades: List<TradeDto>
)