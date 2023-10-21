package uk.co.threebugs.darwinexclient.trade

import uk.co.threebugs.darwinexclient.Type
import uk.co.threebugs.darwinexclient.account.AccountDto
import uk.co.threebugs.darwinexclient.setup.SetupDto
import java.math.BigDecimal
import java.time.ZonedDateTime


data class TradeDto (
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    var type: Type? = null,
    var setup: SetupDto? = null,
    var account: AccountDto? = null,
    var metatraderId: Int? = null,
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
