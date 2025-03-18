// File: src/test/kotlin/uk/co/threebugs/darwinexclient/modifiers/AtrSchedulerIntegrationTest.kt
package uk.co.threebugs.darwinexclient.scheduler

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.co.threebugs.darwinexclient.modifiers.AtrScheduler
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AtrSchedulerIntegrationTest {

    @Autowired
    lateinit var atrScheduler: AtrScheduler

    @Autowired
    lateinit var modifierRepository: ModifierRepository

    companion object {
        // Create the temporary directory as soon as the test class is loaded.
        val tempDir: Path = Files.createTempDirectory("test-files")
        val fileDirProperty: String = tempDir.toAbsolutePath().toString()

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // Set the properties required by the service
            registry.add("app.mql5.files-directories") { fileDirProperty }
            registry.add("app.atr.window") { "3" }
            registry.add("app.atr.type") { "technicalIndicator" }
            registry.add("app.atr.modifierName") { "ATR" }
        }
    }


    @BeforeAll
    fun setup() {
        // Build CSV content with enough rows for the computation.
        val csvContent = """
            Date,Open,High,Low,Close
            2023-01-01,100,110,90,105
            2023-01-02,105,115,95,110
            2023-01-03,110,120,100,115
            2023-01-04,115,125,105,120
            2023-01-05,120,130,110,125
        """.trimIndent()

        // Write the CSV file into the already created temporary directory.
        Files.write(tempDir.resolve("TEST.csv"), csvContent.toByteArray(StandardCharsets.UTF_8))
    }

    @AfterAll
    fun cleanup() {
        // Clean up the temporary directory and its files after tests.
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `should compute and store ATR modifier`() {
        // Manually trigger the computation.
        atrScheduler.computeAndStoreAtr()

        // Query the database for the saved Modifier for symbol "TEST".
        val result = modifierRepository.findBySymbolAndModifierNameAndType(
            symbol = "TEST",
            modifierName = "ATR",
            type = "technicalIndicator"
        )

        // Ensure that the Modifier entity was saved.
        assertNotNull(result, "The ATR Modifier should have been saved to the database.")
        println("Computed ATR Modifier value: ${result?.modifierValue}")
    }
}