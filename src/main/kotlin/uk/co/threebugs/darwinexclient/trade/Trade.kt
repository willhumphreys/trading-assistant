package uk.co.threebugs.darwinexclient.trade

import jakarta.persistence.*
import org.hibernate.envers.*
import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.setup.*
import java.math.*
import java.time.*

@Entity
@Table(name = "trade")
@Audited
data class Trade (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    var createdDateTime: ZonedDateTime? = null,
    var lastUpdatedDateTime: ZonedDateTime? = null,

    @Enumerated(EnumType.STRING)
    var status: Status? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_id")
    var setup: Setup? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    var account: Account? = null,
    var metatraderId: Int? = null,
    var placedDateTime: ZonedDateTime? = null,
    var targetPlaceDateTime: ZonedDateTime? = null,
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
