package uk.co.threebugs.darwinexclient.trade

import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.setup.*
import java.math.*
import java.time.*


data class TradeDto(
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    var lastUpdatedDateTime: ZonedDateTime? = null,
    var status: Status,
    val setup: SetupDto,
    val account: AccountDto,
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
