package uk.co.threebugs.mochiwhattotrade3.trade

import uk.co.threebugs.mochiwhattotrade3.Type
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto
import uk.co.threebugs.mochiwhattotrade3.setup.SetupDto
import java.math.BigDecimal
import java.time.ZonedDateTime


data class TradeDto (
    var id: Int? = null,
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
