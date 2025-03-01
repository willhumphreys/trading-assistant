package uk.co.threebugs.darwinexclient.modifier

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "modifiers")
@EntityListeners(AuditingEntityListener::class)
data class Modifier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "modifier_name", nullable = false, length = 255)
    val modifierName: String,

    @Column(name = "modifier_value", nullable = false, precision = 10, scale = 2)
    val modifierValue: BigDecimal,

    @Column(name = "symbol", nullable = false, length = 255)
    val symbol: String,

    @Column(name = "type", nullable = false, length = 255)
    val type: String,

    @LastModifiedDate
    @Column(name = "last_modified", nullable = false)
    val lastModified: LocalDateTime? = null
)