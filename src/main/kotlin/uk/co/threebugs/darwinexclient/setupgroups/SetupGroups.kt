package uk.co.threebugs.darwinexclient.setupgroups

import jakarta.persistence.*

@Entity
@Table(name = "setup_groups")
class SetupGroups (
    var name: String? = null,
    var scriptsDirectory: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
)
