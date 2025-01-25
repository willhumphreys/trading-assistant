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
    val modifierName: String, // Required field

    @Column(name = "modifier_value", nullable = false, precision = 10, scale = 2) // Updated column name
    val modifierValue: BigDecimal, // Required field

    @Column(name = "symbol", nullable = false, length = 255) // New column for "symbol" set as required
    val symbol: String, // Required field

    @Column(name = "type", nullable = false, length = 255) // New column for "type" set as required
    val type: String // Required field
)