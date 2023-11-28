package uk.co.threebugs.darwinexclient.accountsetupgroups

import com.fasterxml.jackson.annotation.*
import uk.co.threebugs.darwinexclient.tradingstance.*


data class AccountSetupGroupsFileDto(
    val name: String,
    @JsonProperty("setup-group-name") val setupGroupName: String,
    @JsonProperty("metatrader-account") val metatraderAccount: String,
    @JsonProperty("trading-stances") val tradingStances: List<TradingStanceFileDto>
)
