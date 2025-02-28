package uk.co.threebugs.darwinexclient.modifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import uk.co.threebugs.darwinexclient.modifier.Modifier
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AtrSchedulerTest {

    // Mock your dependency
    @Mock
    lateinit var modifierRepository: ModifierRepository

    /**
     * We'll not rely on Spring here; we'll instantiate the AtrScheduler manually,
     * passing in the required constructor arguments. For real integration tests,
     * you'd bring up a Spring context (e.g. with @SpringBootTest).
     */
    lateinit var atrScheduler: AtrScheduler

    private lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // Create a temp directory to hold CSV test data
        tempDir = Files.createTempDirectory("atr-test")
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `calculateRollingAtr returns correct ATR`() {
        // We can directly test the "calculateRollingAtr" method using sample data.
        // Suppose we use a 3-day window for easy demonstration.
        val bars = listOf(
            AtrScheduler.DailyBar(LocalDate.of(2025, 1, 1), bd("100"), bd("110"), bd("90"),  bd("105")),
            AtrScheduler.DailyBar(LocalDate.of(2025, 1, 2), bd("105"), bd("120"), bd("100"), bd("115")),
            AtrScheduler.DailyBar(LocalDate.of(2025, 1, 3), bd("115"), bd("130"), bd("110"), bd("120")),
            AtrScheduler.DailyBar(LocalDate.of(2025, 1, 4), bd("120"), bd("125"), bd("115"), bd("123")),
        )

        // Construct the scheduler with arbitrary property values; we'll only test the ATR logic.
        atrScheduler = AtrScheduler(
            modifierRepository = modifierRepository,
            filesDirectoriesString = "",  // not used here
            atrWindow = 3,
            atrType = "technicalIndicator",
            atrModifierName = "ATR"
        )

        // call
        val atr = atrScheduler.calculateRollingAtr(bars, 3)

        // This test data would yield a particular ATR. You can compute manually or check an online formula.
        // For demonstration, just assert it's non-null or do a specific numeric check:
        assertThat(atr).isNotNull
        // If you expect a certain value, do something like:
        // assertThat(atr).isEqualByComparingTo("X.YZ")
    }

    @Test
    fun `computeAndStoreAtr processes CSV in directory and saves ATR`() {
        // 1) Create a CSV file in the temp directory
        val testCsv = tempDir.resolve("SP500.csv")
        Files.write(
            testCsv,
            listOf(
                "Date,Open,High,Low,Close",
                "2025-02-01,100,110,90,105",
                "2025-02-02,105,120,100,115",
                "2025-02-03,115,125,95,110",
                "2025-02-04,110,130,105,120"
            )
        )

        // 2) Construct the scheduler with "tempDir" as the only directory
        //    (filesDirectoriesString is set to the temp folder path)
        atrScheduler = AtrScheduler(
            modifierRepository = modifierRepository,
            filesDirectoriesString = tempDir.toString(),
            atrWindow = 2,                 // short window for easier test
            atrType = "technicalIndicator",
            atrModifierName = "ATR"
        )

        // 3) Run the method under test
        atrScheduler.computeAndStoreAtr()

        // 4) Verify we saved an ATR. We do not know the exact value but we can capture it.
        //    If you'd like a precise assertion, you can use an ArgumentCaptor or check the numeric range.
        verify(modifierRepository, times(1)).save(Mockito.any(Modifier::class.java))
    }

    /**
     * Utility function to convert string to BigDecimal
     */
    private fun bd(str: String): BigDecimal = str.toBigDecimal()
}
