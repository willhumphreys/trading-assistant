package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CurrencyInfo(
    val last: BigDecimal,
    val ask: BigDecimal,
    val bid: BigDecimal,
    @JsonProperty("tick_value")
    val tickValue: BigDecimal
)