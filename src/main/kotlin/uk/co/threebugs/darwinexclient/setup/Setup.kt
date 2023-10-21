package uk.co.threebugs.darwinexclient.setup

import jakarta.persistence.*
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import java.time.ZonedDateTime

@Entity
@Table(name = "setup")
class Setup (
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
    var outOfTime: Int? = null) {

    @PrePersist
    fun prePersist() {
        createdDateTime = ZonedDateTime.now()
    }

    val isLong: Boolean
        get() = stop!! < limit!!

    fun concatenateFields(): String {
        return "id: $id- setupGroup: ${setupGroup?.id}- symbol: $symbol- rank: $rank- dayOfWeek: $dayOfWeek- hourOfDay: $hourOfDay- stop: $stop- limit: $limit- tickOffset: $tickOffset- tradeDuration: $tradeDuration- outOfTime: $outOfTime- isLong: $direction"
                .replace(",", "-")
    }

    val direction: String
        get() = if (stop!! < limit!!) "LONG" else "SHORT"
}
