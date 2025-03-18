package uk.co.threebugs.darwinexclient.modifier

import java.math.BigDecimal


data class ModifierJson(

    val modifierName: String,
    val modifierValue: BigDecimal,
    val symbol: String,
    val type: String,
)