package uk.co.threebugs.darwinexclient.setupmodifier

import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import uk.co.threebugs.darwinexclient.modifier.Modifier

interface SetupModifierRepository : JpaRepository<SetupModifier, Int> {

    @Query("""
        SELECT m
        FROM Modifier m
        JOIN SetupModifier sm ON m.id = sm.modifierId
        WHERE sm.setupId = :setupId
    """)
    fun findModifiersBySetupId(@Param("setupId") setupId: Int): List<Modifier>
}