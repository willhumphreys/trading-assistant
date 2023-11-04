package uk.co.threebugs.darwinexclient.setupgroup

import jakarta.persistence.*
import org.hibernate.envers.*
import uk.co.threebugs.darwinexclient.setupgroups.*

@Entity
@Table(name = "setup_group")
@Audited
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
)
