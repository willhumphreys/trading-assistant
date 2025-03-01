package uk.co.threebugs.darwinexclient.modifiers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.modifier.Modifier
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Path
import java.time.LocalDateTime

@Service
class ModifierJsonUpdaterService(
    private val modifierRepository: ModifierRepository
) {
    private val objectMapper = jacksonObjectMapper() // Jackson object mapper

    /**
     * Reads a JSON file and applies multipliers to your Modifier table.
     * If a matching Modifier doesn't exist, creates a new one.
     */
    fun updateModifiersFromJsonFile(filePath: Path) {
        val file = filePath.toFile()
        if (!file.exists()) {
            println("JSON modifier file not found at: $filePath")
            return
        }

        // 1) Parse the JSON into a list of ModifierUpdateDto
        val updates: List<Modifier> = try {
            objectMapper.readValue(file)
        } catch (ex: Exception) {
            println("Failed to parse JSON file: ${ex.message}")
            return
        }

        // 2) Process each update in the JSON
        updates.forEach { dto ->
            val existing = modifierRepository.findBySymbolAndModifierNameAndType(
                symbol = dto.symbol, modifierName = dto.modifierName, type = dto.type
            )

            if (existing != null) {


                val updatedModifier = existing.copy(modifierValue = dto.modifierValue)
                modifierRepository.save(updatedModifier)

                println(
                    "Updated ${dto.modifierName} for symbol=${dto.symbol} " + "type=${dto.type} to $dto.modifierValue (multiplier=${dto.modifierValue})."
                )
            } else {
                // Create a new Modifier record using the multiplier as the initial value
                val newValue = BigDecimal.ONE.multiply(dto.modifierValue).setScale(2, RoundingMode.HALF_UP)

                val newModifier = Modifier(
                    modifierName = dto.modifierName,
                    modifierValue = newValue,
                    symbol = dto.symbol,
                    type = dto.type,
                    lastModified = LocalDateTime.now()
                )
                modifierRepository.save(newModifier)

                println(
                    "Created new Modifier: name=${dto.modifierName}, " + "symbol=${dto.symbol}, type=${dto.type}, value=$newValue."
                )
            }
        }
    }
}
