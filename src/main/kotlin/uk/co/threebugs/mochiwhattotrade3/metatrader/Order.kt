package uk.co.threebugs.mochiwhattotrade3.metatrader

import java.math.BigDecimal


data class Order(
        val symbol: String? = null,
        val orderType: String? = null,
        val lots: Double? = null,
        val price: BigDecimal? = null,
        val stopLoss: BigDecimal? = null,
        val takeProfit: BigDecimal? = null,
        val magic: Int? = null,
        val expiration: Long? = null,
        val comment: String? = null
)