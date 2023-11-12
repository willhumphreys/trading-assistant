package uk.co.threebugs.darwinexclient.tradingstance

data class TradingStanceInfo(
    val tradingStance: TradingStance,
    val enabledSetupCount: Long,
    val disabledSetupCount: Long
)
