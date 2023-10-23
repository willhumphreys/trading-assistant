package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AccountInfo(

    var number: Int,
    var leverage: Int,
    var balance: BigDecimal,
    @JsonProperty("free_margin")
    var freeMargin: BigDecimal,
    var name: String,
    var currency: String,
    var equity: BigDecimal
)
