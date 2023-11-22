package uk.co.threebugs.darwinexclient.tradingstance

import uk.co.threebugs.darwinexclient.setupgroup.*

data class UpdateTradingStanceDto(
    val symbol: String,
    val direction: Direction,
    val accountSetupGroupsName: String
)