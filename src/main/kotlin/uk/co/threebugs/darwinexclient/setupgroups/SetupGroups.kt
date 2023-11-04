package uk.co.threebugs.darwinexclient.setupgroups

import jakarta.persistence.*
import org.hibernate.envers.*

@Entity
@Table(name = "setup_groups")
@Audited
class SetupGroups (
    var name: String? = null,
    var scriptsDirectory: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
)
