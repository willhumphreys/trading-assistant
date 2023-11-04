package uk.co.threebugs.darwinexclient.account

import com.fasterxml.jackson.annotation.JsonProperty

data class MetaTraderDir(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("dir-path")
    val dirPath: String
)
