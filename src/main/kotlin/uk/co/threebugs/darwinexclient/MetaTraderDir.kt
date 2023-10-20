package uk.co.threebugs.darwinexclient

import com.fasterxml.jackson.annotation.JsonProperty

class MetaTraderDir {
    // Getters and setters
    @JsonProperty("name")
    val name: String? = null

    @JsonProperty("dir-path")
    val dirPath: String? = null
}
