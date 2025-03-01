package uk.co.threebugs.darwinexclient.setupmodifier

import jakarta.persistence.*
import java.time.LocalDateTime
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "setup_modifiers")
@EntityListeners(AuditingEntityListener::class)
data class SetupModifier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "setup_id", nullable = false)
    val setupId: Int,

    @Column(name = "modifier_id", nullable = false)
    val modifierId: Int,

    @LastModifiedDate
    @Column(name = "last_modified", nullable = false)
    val lastModified: LocalDateTime? = null
)