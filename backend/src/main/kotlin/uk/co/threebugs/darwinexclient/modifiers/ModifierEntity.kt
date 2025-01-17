package uk.co.threebugs.darwinexclient.modifier

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "modifiers")
data class Modifier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0, // Auto-incremented primary key

    @Column(nullable = false, length = 255)
    val name: String, // Name of the modifier

    @Column(nullable = false, precision = 10, scale = 2) // Precisely defines the scale for the value
    val value: BigDecimal // Using BigDecimal for exact precision
)