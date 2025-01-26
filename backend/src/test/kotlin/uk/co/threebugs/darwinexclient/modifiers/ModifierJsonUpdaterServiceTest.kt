package uk.co.threebugs.darwinexclient.modifiers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import uk.co.threebugs.darwinexclient.modifier.Modifier
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
@DisplayName("ModifierJsonUpdaterServiceTest")
class ModifierJsonUpdaterServiceTest {

    private lateinit var modifierRepository: ModifierRepository
    private lateinit var modifierJsonUpdaterService: ModifierJsonUpdaterService
    private val objectMapper = jacksonObjectMapper() // Jackson object mapper for testing JSON

    @BeforeEach
    fun setup() {
        // Mock ModifierRepository
        modifierRepository = mock()

        // Initialize the service with mocked repository
        modifierJsonUpdaterService = ModifierJsonUpdaterService(modifierRepository)
    }

    @Test
    @DisplayName("should update existing modifier when matching entry exists")
    fun `should update existing modifier when matching entry exists`() {
        // Arrange
        val jsonFilePath = createTempJsonFile(
            """
            [
                { 
                    "modifierName": "ATR", 
                    "modifierValue": 1.50, 
                    "symbol": "EURUSD", 
                    "type": "technicalIndicator" 
                }
            ]
            """.trimIndent()
        )
        val existingModifier = Modifier(
            modifierName = "ATR",
            modifierValue = BigDecimal("2.00"),
            symbol = "EURUSD",
            type = "technicalIndicator"
        )

        whenever(
            modifierRepository.findBySymbolAndModifierNameAndType(
                eq("EURUSD"),
                eq("ATR"),
                eq("technicalIndicator")
            )
        ).thenReturn(existingModifier)

        // Act
        modifierJsonUpdaterService.updateModifiersFromJsonFile(jsonFilePath)

        // Assert
        val updatedModifierCaptor = argumentCaptor<Modifier>()
        verify(modifierRepository).save(updatedModifierCaptor.capture())

        val savedModifier = updatedModifierCaptor.firstValue
        val expectedValue = BigDecimal("1.50").setScale(2, RoundingMode.HALF_UP)

        assertThat(savedModifier.modifierValue).isEqualTo(expectedValue)
        assertThat(savedModifier.symbol).isEqualTo("EURUSD")
        assertThat(savedModifier.modifierName).isEqualTo("ATR")
        assertThat(savedModifier.type).isEqualTo("technicalIndicator")
    }

    @Test
    @DisplayName("should create new modifier when no matching entry exists")
    fun `should create new modifier when no matching entry exists`() {
        // Arrange
        val jsonFilePath = createTempJsonFile(
            """
            [
                {
                    "modifierName": "ATR",
                    "modifierValue": 1.75,
                    "symbol": "GBPUSD",
                    "type": "technicalIndicator"
                }
            ]
            """.trimIndent()
        )

        whenever(
            modifierRepository.findBySymbolAndModifierNameAndType(
                eq("GBPUSD"),
                eq("ATR"),
                eq("technicalIndicator")
            )
        ).thenReturn(null) // No existing modifier

        // Act
        modifierJsonUpdaterService.updateModifiersFromJsonFile(jsonFilePath)

        // Assert
        val newModifierCaptor = argumentCaptor<Modifier>()
        verify(modifierRepository).save(newModifierCaptor.capture())

        val savedModifier = newModifierCaptor.firstValue
        val expectedValue = BigDecimal("1.75").setScale(2, RoundingMode.HALF_UP)

        assertThat(savedModifier.modifierValue).isEqualTo(expectedValue)
        assertThat(savedModifier.symbol).isEqualTo("GBPUSD")
        assertThat(savedModifier.modifierName).isEqualTo("ATR")
        assertThat(savedModifier.type).isEqualTo("technicalIndicator")
    }

    @Test
    @DisplayName("should handle malformed JSON file gracefully")
    fun `should handle malformed JSON file gracefully`() {
        // Arrange
        val jsonFilePath = createTempJsonFile("{ malformed json")

        // Act
        modifierJsonUpdaterService.updateModifiersFromJsonFile(jsonFilePath)

        // Assert
        verifyNoInteractions(modifierRepository) // Repository should not be called on failure
    }

    @Test
    @DisplayName("should log message when JSON file does not exist")
    fun `should log message when JSON file does not exist`() {
        // Arrange
        val nonExistentPath = Path.of("non-existent-file.json")

        // Act
        modifierJsonUpdaterService.updateModifiersFromJsonFile(nonExistentPath)

        // Assert
        verifyNoInteractions(modifierRepository) // No interaction with mock repository
    }

    /*
     * Utility to create a temporary JSON file with the given content for testing purposes.
     */
    private fun createTempJsonFile(content: String): Path {
        val tempFile = File.createTempFile("modifiers", ".json")
        tempFile.writeText(content)
        tempFile.deleteOnExit() // Clean up the temp file after the test runs
        return tempFile.toPath()
    }
}
