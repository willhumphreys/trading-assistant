package uk.co.threebugs.darwinexclient.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.modifiers.AtrScheduler
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import java.math.BigDecimal

/**
 * Integration test that:
 *  - loads test CSV data from src/test/resources/mql5testfiles/SP500.csv,
 *  - computes 14-day ATR via AtrScheduler,
 *  - verifies the result in the H2 database.
 */
@SpringBootTest
@TestPropertySource(properties = [
    // Points to the folder containing our CSV test files:
    "app.mql5.files-directory=src/test/resources/mql5testfiles",
    // Ensure we use a 14-day ATR window:
    "app.atr.window=14"
])
class AtrSchedulerIntegrationTest(
    @Autowired val atrScheduler: AtrScheduler,
    @Autowired val modifierRepository: ModifierRepository
) {

    @Test
    fun `test 14-day ATR calculation for SP500 CSV`() {
        // Trigger the calculation
        atrScheduler.computeAndStoreAtr()

        // Find the newly created or updated Modifier for SP500
        val allModifiers: List<Modifier> = modifierRepository.findAll()
        val sp500Modifier = allModifiers.find { it.symbol == "sp500" && it.modifierName == "ATR" }

        // Assert the SP500 Modifier is not null
        assertThat(sp500Modifier)
            .withFailMessage("ATR Modifier for SP500 should have been created or updated.")
            .isNotNull()

        // Assert that the ATR Modifier value is greater than 0
        assertThat(sp500Modifier?.modifierValue)
            .withFailMessage("Expected ATR value to be > 0, but got: ${sp500Modifier?.modifierValue}")
            .isGreaterThan(BigDecimal.ZERO)

        // **Optional**: Replace the value below with the expected ATR for the `SP500.csv` file.
        assertThat(sp500Modifier?.modifierValue)
            .isEqualTo(BigDecimal("69.510000"))
    }
}