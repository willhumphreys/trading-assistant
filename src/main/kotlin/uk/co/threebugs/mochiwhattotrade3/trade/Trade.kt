package uk.co.threebugs.mochiwhattotrade3.trade

import jakarta.persistence.*
import uk.co.threebugs.mochiwhattotrade3.Type
import uk.co.threebugs.mochiwhattotrade3.account.Account
import uk.co.threebugs.mochiwhattotrade3.setup.Setup
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "trade")
data class Trade (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    //@Column(name = "type", columnDefinition = "ENUM('BUY', 'SELL', 'HOLD')")
    @Enumerated(EnumType.STRING)
    var type: Type? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_id")
    var setup: Setup? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    var account: Account? = null,
    var metatraderId: Int? = null,
    var placedDateTime: ZonedDateTime? = null,
    var placedPrice: BigDecimal? = null,
    var filledDateTime: ZonedDateTime? = null,
    var filledPrice: BigDecimal? = null,
    var closedDateTime: ZonedDateTime? = null,
    var closedPrice: BigDecimal? = null,
    var closeType: String? = null,
    var profit: BigDecimal? = null,
    @Column(name = "message_column")
    var message: String? = null
)

{
val newTradeMessage: String
    get() = "New Trade: ${setup?.rank} ${setup?.symbol} ${setup?.direction} in account: ${account?.name}"

}
