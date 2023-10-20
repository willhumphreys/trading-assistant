package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime


data class TradeInfo (
    var magic: Int? = null,
    var lots: BigDecimal? = null,
    var symbol: String? = null,
    var swap: BigDecimal? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    @JsonProperty("open_time")
    var openTime: LocalDateTime? = null,

    @JsonProperty("SL")
    var stopLoss: BigDecimal? = null,
    var comment: String? = null,
    var type: String? = null,

    @JsonProperty("open_price")
    var openPrice: BigDecimal? = null,

    @JsonProperty("TP")
    var takeProfit: BigDecimal? = null,

    @JsonProperty("pnl")
    var profitAndLoss: BigDecimal? = null,
    var mapType: Any? = null,
    var empty: Boolean? = null
)

