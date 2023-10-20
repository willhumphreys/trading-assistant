package uk.co.threebugs.mochiwhattotrade3.account

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

