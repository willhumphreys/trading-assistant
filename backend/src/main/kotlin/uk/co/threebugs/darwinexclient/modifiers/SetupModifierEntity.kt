package uk.co.threebugs.darwinexclient.setupmodifier

import jakarta.persistence.*

@Entity
@Table(name = "setup_modifiers")
data class SetupModifier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0, // Auto-incremented primary key

    @Column(name = "setup_id", nullable = false)
    val setupId: Int, // Foreign key referencing the `setup` table

    @Column(name = "modifier_id", nullable = false)
    val modifierId: Int // Foreign key referencing the `modifiers` table
)