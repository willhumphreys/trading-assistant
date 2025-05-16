package uk.co.threebugs.darwinexclient.setup

import jakarta.persistence.*
import org.hibernate.envers.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import java.time.*

@Entity
@Table(name = "setup")
@Audited
class Setup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,

    @ManyToOne(fetch = FetchType.EAGER)
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

    @Column(name = "active", nullable = false)
    var active: Boolean = true // Added active field with default true
) {

    @PrePersist
    fun prePersist() {
        createdDateTime = ZonedDateTime.now()
        // 'active' will take its default value 'true' if not explicitly set
    }

    val isLong: Boolean
        get() = stop!! < limit!! // Consider handling nullable stop/limit more safely if they can be null

    fun concatenateFields(): String {
        // It's good practice to handle potential nulls in setupGroup and setupGroup.setupGroups
        val setupGroupId = setupGroup?.id ?: "null"
        val setupGroupsId = setupGroup?.setupGroups?.id ?: "null"
        return "setupId: $id- setupGroup: $setupGroupId- symbol: $symbol- rank: $rank- isLong: $direction setupGroups: $setupGroupsId"
            .replace(",", "-")
    }

    val direction: Direction
        get() = if (stop!! < limit!!) Direction.LONG else Direction.SHORT // Same as isLong, consider null safety
}