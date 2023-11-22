package uk.co.threebugs.darwinexclient.trade

import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.account.AccountDto
import uk.co.threebugs.darwinexclient.setup.SetupDto
import java.math.BigDecimal
import java.time.ZonedDateTime


data class TradeDto(
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    var lastUpdatedDateTime: ZonedDateTime? = null,
    var status: Status,
    val setup: SetupDto,
    val account: AccountDto,
    var metatraderId: Int? = null,
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
