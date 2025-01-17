package uk.co.threebugs.darwinexclient.modifier

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "modifiers")
data class Modifier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "modifier_name", nullable = false, length = 255) // Updated column name
    val modifierName: String, // Updated field name

    @Column(name = "modifier_value", nullable = false, precision = 10, scale = 2) // Updated column name
    val modifierValue: BigDecimal // Updated field name
)