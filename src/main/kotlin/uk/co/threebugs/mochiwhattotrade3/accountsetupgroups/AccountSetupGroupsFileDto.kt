package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups

import com.fasterxml.jackson.annotation.JsonProperty


data class AccountSetupGroupsFileDto(val name: String,
                                     @JsonProperty("setup-group-name") val setupGroupName: String,
                                     @JsonProperty("metatrader-account") val metatraderAccount: String
)
