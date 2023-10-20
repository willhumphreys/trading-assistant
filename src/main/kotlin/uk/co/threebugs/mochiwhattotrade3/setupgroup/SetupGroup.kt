package uk.co.threebugs.mochiwhattotrade3.setupgroup

import jakarta.persistence.*
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups

@Entity
@Table(name = "setup_group")
class SetupGroup (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_groups_id")
    var setupGroups: SetupGroups? = null,
    var path: String? = null,
    var symbol: String? = null,
    var enabled: Boolean? = null,
){
}
