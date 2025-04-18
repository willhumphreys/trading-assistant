package uk.co.threebugs.darwinexclient.modifiers

import org.springframework.data.jpa.repository.JpaRepository
import uk.co.threebugs.darwinexclient.modifier.Modifier

interface ModifierRepository : JpaRepository<Modifier, Int> {
    fun findBySymbolAndModifierNameAndType(
        symbol: String,
        modifierName: String,
        type: String
    ): Modifier?
}