package uk.co.threebugs.darwinexclient.tradingstance

import jakarta.persistence.*
import org.hibernate.envers.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.setupgroup.*

@Entity
@Table(name = "trading_stance")
@Audited
class TradingStance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var symbol: String? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_setup_groups_id")
    var accountSetupGroups: AccountSetupGroups? = null,
    @Enumerated(EnumType.STRING)
    var direction: Direction? = null
) {
    override fun toString(): String {
        return "TradingStance(id=$id, symbol=$symbol, direction=$direction, accountSetupGroups=${accountSetupGroups?.toString()})"
    }
}