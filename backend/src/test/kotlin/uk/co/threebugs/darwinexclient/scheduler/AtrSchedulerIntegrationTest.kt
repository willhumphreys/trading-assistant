package uk.co.threebugs.darwinexclient.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import java.math.BigDecimal

/**
 * Integration test that:
 *  - loads test CSV data from src/test/resources/mql5testfiles/EURUSD.csv,
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
    fun `test 14-day ATR calculation on 30-row CSV`() {
        // Trigger the calculation
        atrScheduler.computeAndStoreAtr()

        // Find the newly created or updated Modifier for EURUSD
        val allModifiers: List<Modifier> = modifierRepository.findAll()
        val eurUsdModifier = allModifiers.find { it.symbol == "EURUSD" && it.modifierName == "ATR" }

        // Assert the EURUSD Modifier is not null
        assertThat(eurUsdModifier)
            .withFailMessage("ATR Modifier for EURUSD should have been created or updated.")
            .isNotNull()

        // Assert that the ATR Modifier value is greater than 0
        assertThat(eurUsdModifier?.modifierValue)
            .withFailMessage("Expected ATR value to be > 0, but got: ${eurUsdModifier?.modifierValue}")
            .isGreaterThan(BigDecimal.ZERO)

        assertThat(eurUsdModifier?.modifierValue).isEqualTo(BigDecimal("0.021430"))
    }
}