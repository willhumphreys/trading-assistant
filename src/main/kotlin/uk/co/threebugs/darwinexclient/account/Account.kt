package uk.co.threebugs.darwinexclient.account

import jakarta.persistence.*
import org.hibernate.envers.*

@Entity
@Table(name = "account")
@Audited
class Account (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var name: String?,
    var metatraderAdvisorPath: String?
)

