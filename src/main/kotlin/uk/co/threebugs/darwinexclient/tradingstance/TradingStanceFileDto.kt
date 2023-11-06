package uk.co.threebugs.darwinexclient.tradingstance

import uk.co.threebugs.darwinexclient.setupgroup.*

data class TradingStanceFileDto(
    val symbol: String,
    val accountName: String,
    val direction: Direction
)