package uk.co.threebugs.darwinexclient.setup

import jakarta.persistence.*
import org.hibernate.envers.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import java.time.*
import jakarta.persistence.CascadeType;

@Entity
@Table(name = "setup")
@Audited
class Setup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "setup_group_id")
    var setupGroup: SetupGroup? = null,
    var symbol: String? = null,

    @Column(name = "rank_column")
    var rank: Int? = null,
    var dayOfWeek: Int? = null,
    var hourOfDay: Int? = null,

    @Column(name = "stop_column")
    var stop: Int? = null,

    @Column(name = "limit_column")
    var limit: Int? = null,
    var tickOffset: Int? = null,
    var tradeDuration: Int? = null,
    var outOfTime: Int? = null,
    var name: String? = null,
    var enabled: Boolean = true
) {

    @PrePersist
    fun prePersist() {
        createdDateTime = ZonedDateTime.now()
    }

    val isLong: Boolean
        get() = stop!! < limit!!

    fun concatenateFields(): String {
        return "setupId: $id- setupGroup: ${setupGroup?.id}- symbol: $symbol- rank: $rank- isLong: $direction setupGroups: ${setupGroup!!.setupGroups!!.id}"
            .replace(",", "-")
    }

    val direction: Direction
        get() = if (stop!! < limit!!) Direction.LONG else Direction.SHORT
}
