package uk.co.threebugs.darwinexclient.account

import jakarta.persistence.*

@Entity
@Table(name = "account")
class Account (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var name: String?,
    var metatraderAdvisorPath: String?
)

