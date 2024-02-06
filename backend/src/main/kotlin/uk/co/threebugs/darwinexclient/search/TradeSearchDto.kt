package uk.co.threebugs.darwinexclient.search

import uk.co.threebugs.darwinexclient.*
import java.math.*
import java.time.*


data class TradeSearchDto(
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    var lastUpdatedDateTime: ZonedDateTime? = null,
    var status: Status? = null,
    var setup: SetupSearchDto? = null,
    var account: AccountSearchDto? = null,
    var metatraderId: Long? = null,
    var targetPlaceDateTime: ZonedDateTime? = null,
    var placedDateTime: ZonedDateTime? = null,
    var placedPrice: BigDecimal? = null,
    var filledDateTime: ZonedDateTime? = null,
    var filledPrice: BigDecimal? = null,
    var closedDateTime: ZonedDateTime? = null,
    var closedPrice: BigDecimal? = null,
    var profit: BigDecimal? = null,
    var closeType: String? = null,
    var message: String? = null
)
