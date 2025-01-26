package uk.co.threebugs.darwinexclient.setup

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.junit.jupiter.MockitoExtension
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifierRepository
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
@DisplayName("SetupFileRepositoryTest")
class SetupFileRepositoryTest {

    private lateinit var setupModifierRepository: SetupModifierRepository
    private lateinit var modifierRepository: ModifierRepository
    private lateinit var setupFileRepository: SetupFileRepository

    @BeforeEach
    fun init() {
        setupModifierRepository = mock()
        modifierRepository = mock()
        setupFileRepository = SetupFileRepository(setupModifierRepository, modifierRepository)
    }

    @Test
    @DisplayName("should read CSV and create ParsedSetupWithModifier objects up to the specified limit")
    fun shouldReadCsvAndCreateSetupObjectsUpToLimit() {
        // Arrange
        val symbol = "EURUSD"
        val setupGroup = mock<SetupGroup>()
        // CSV with header + 2 lines of data (no meaningful modifier name)
        val csvContent = """
            "rank","traderId","dayOfWeek","hourOfDay","stop","limit","tickOffset","duration","outOfTime"
            10,1322294,3,16,100,200,50,360,0
            11,789456,5,12,150,300,75,180,1
        """.trimIndent()

        val tempCsv = createTempCsv(csvContent)

        // Act
        val result = setupFileRepository.readCsv(
            path = tempCsv,
            symbol = symbol,
            setupGroup = setupGroup,
            setupLimit = 2
        )

        // Assert
        // Returns List<ParsedSetupWithModifier>
        assertThat(result).hasSize(2)

        // First row
        val firstParsed = result.first()
        val firstSetup = firstParsed.setup
        assertThat(firstSetup.rank).isEqualTo(10)
        assertThat(firstSetup.dayOfWeek).isEqualTo(3)
        assertThat(firstSetup.hourOfDay).isEqualTo(16)
        assertThat(firstSetup.stop).isEqualTo(100)
        assertThat(firstSetup.limit).isEqualTo(200)
        assertThat(firstSetup.tickOffset).isEqualTo(50)
        assertThat(firstSetup.tradeDuration).isEqualTo(360)
        assertThat(firstSetup.outOfTime).isEqualTo(0)
        assertThat(firstSetup.symbol).isEqualTo(symbol)
        assertThat(firstSetup.setupGroup).isEqualTo(setupGroup)
        // No modifier column => no modifier
        assertThat(firstParsed.modifier).isNull()

        // Second row
        val secondParsed = result[1]
        val secondSetup = secondParsed.setup
        assertThat(secondSetup.rank).isEqualTo(11)
        assertThat(secondSetup.dayOfWeek).isEqualTo(5)
        assertThat(secondSetup.hourOfDay).isEqualTo(12)
        assertThat(secondSetup.stop).isEqualTo(150)
        assertThat(secondSetup.limit).isEqualTo(300)
        assertThat(secondSetup.tickOffset).isEqualTo(75)
        assertThat(secondSetup.tradeDuration).isEqualTo(180)
        assertThat(secondSetup.outOfTime).isEqualTo(1)
        assertThat(secondSetup.symbol).isEqualTo(symbol)
        assertThat(secondSetup.setupGroup).isEqualTo(setupGroup)
        // No modifier column => no modifier
        assertThat(secondParsed.modifier).isNull()

        // Because there's no modifier column, we expect no interactions
        verifyNoInteractions(modifierRepository, setupModifierRepository)
    }

    @Test
    @DisplayName("should not exceed maximum setup limit and throw RuntimeException if limit is above MAX_SETUP_LIMIT")
    fun shouldThrowIfSetupLimitExceedsMax() {
        // Arrange
        val symbol = "GBPUSD"
        val setupGroup = mock<SetupGroup>()

        // Act & Assert
        assertThatThrownBy {
            setupFileRepository.readCsv(
                path = Path.of("unused.csv"),
                symbol = symbol,
                setupGroup = setupGroup,
                setupLimit = 999 // Over 20
            )
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("setupLimit cannot be greater than")
    }

    @Test
    @DisplayName("should handle modifier column by looking up existing modifier or creating new")
    fun shouldHandleModifierColumn() {
        // Arrange
        val symbol = "USDJPY"
        val setupGroup = mock<SetupGroup>()
        val existingModifier = Modifier(
            id = 100,
            modifierName = "ATR",
            modifierValue = BigDecimal("2.00"),
            symbol = symbol,
            type = "technicalIndicator"
        )
        // CSV has a 10th column with "ATR" on the first line and "NEW_MOD" on the second.
        val csvContent = """
            "rank","traderId","dayOfWeek","hourOfDay","stop","limit","tickOffset","duration","outOfTime","modifierColumn"
            8,1322294,1,10,7350,-18375,245,336,8,ATR
            9,852895,1,15,6860,-18375,0,336,8,NEW_MOD
        """.trimIndent()

        val tempCsv = createTempCsv(csvContent)

        // For "ATR" => existingModifier found
        whenever(
            modifierRepository.findBySymbolAndModifierNameAndType(
                eq(symbol),
                eq("ATR"),
                eq("technicalIndicator")
            )
        ).thenReturn(existingModifier)

        // For "NEW_MOD" => not found => new must be created
        whenever(
            modifierRepository.findBySymbolAndModifierNameAndType(
                eq(symbol),
                eq("NEW_MOD"),
                eq("technicalIndicator")
            )
        ).thenReturn(null)

        whenever(modifierRepository.save(any<Modifier>())).thenAnswer { invocation ->
            val modifierToSave = invocation.arguments[0] as Modifier
            // You can modify modifierToSave if needed (e.g., set an ID)
            modifierToSave.copy(id = 999) // Example: set the ID to 999
        }


        // Act
        val result = setupFileRepository.readCsv(
            path = tempCsv,
            symbol = symbol,
            setupGroup = setupGroup,
            setupLimit = 2
        )

        // Assert: we get 2 ParsedSetupWithModifier entries
        assertThat(result).hasSize(2)

        // First row should return existingModifier
        val firstRow = result.first()
        assertThat(firstRow.setup.rank).isEqualTo(8)
        assertThat(firstRow.modifier).isSameAs(existingModifier)

        // Second row => newly created modifier
        val secondRow = result[1]
        assertThat(secondRow.setup.rank).isEqualTo(9)

        // We capture the newly-created modifier that is being saved
        val modifierCaptor = argumentCaptor<Modifier>()
        verify(modifierRepository).save(modifierCaptor.capture())

        val newlyCreated = modifierCaptor.firstValue
        assertThat(newlyCreated.modifierName).isEqualTo("NEW_MOD")
        assertThat(newlyCreated.symbol).isEqualTo(symbol)
        assertThat(newlyCreated.type).isEqualTo("technicalIndicator")
        assertThat(newlyCreated.modifierValue).isEqualTo(BigDecimal("-1.00"))

        // The second row's modifier should be the "saved" instance, with ID=999
        assertThat(secondRow.modifier).isNotNull
        assertThat(secondRow.modifier?.modifierName).isEqualTo("NEW_MOD")
        assertThat(secondRow.modifier?.symbol).isEqualTo(symbol)
        assertThat(secondRow.modifier?.id).isEqualTo(999)

        // This repository no longer saves SetupModifier, so verify no interactions:
        verifyNoInteractions(setupModifierRepository)
    }

    @Test
    @DisplayName("should throw RuntimeException on IOException")
    fun shouldThrowRuntimeExceptionOnIoError() {
        // Arrange
        val symbol = "USDCHF"
        val setupGroup = mock<SetupGroup>()
        // Provide a non-existing path (forces IOException)
        val nonexistentPath = Path.of("this-file-does-not-exist.csv")

        // Act & Assert
        assertThatThrownBy {
            setupFileRepository.readCsv(
                path = nonexistentPath,
                symbol = symbol,
                setupGroup = setupGroup,
                setupLimit = 1
            )
        }.isInstanceOf(RuntimeException::class.java)
            .hasCauseInstanceOf(java.nio.file.NoSuchFileException::class.java)

        // We do not call the repos if we fail to read
        verifyNoInteractions(modifierRepository, setupModifierRepository)
    }

    /**
     * Utility method to create a temporary CSV file for testing.
     */
    private fun createTempCsv(content: String): Path {
        val tempFile = File.createTempFile("setupfile", ".csv")
        tempFile.writeText(content)
        tempFile.deleteOnExit()
        return tempFile.toPath()
    }
}
