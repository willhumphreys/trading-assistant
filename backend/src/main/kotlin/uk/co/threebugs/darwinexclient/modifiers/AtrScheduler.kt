package uk.co.threebugs.darwinexclient.modifiers

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.utils.logger
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence

@Service
class AtrScheduler(
    private val modifierRepository: ModifierRepository,

    // Remove the old single directory injection
    // @Value("\${app.mql5.files-directory}") private val filesDirectory: String,

    // New property with a list of directories
    @Value("\${app.mql5.files-directories}") private val filesDirectoriesString: String,

    @Value("\${app.atr.window:14}") private val atrWindow: Int,
    @Value("\${app.atr.type:technicalIndicator}") private val atrType: String,
    @Value("\${app.atr.modifierName:ATR}") private val atrModifierName: String
) {

    data class DailyBar(
        val date: LocalDate, val open: BigDecimal, val high: BigDecimal, val low: BigDecimal, val close: BigDecimal
    )

    @Scheduled(cron = "0 0 0 * * ?")
    fun computeAndStoreAtr() {

        try {

            // Parse out the directories from the property (comma-separated)
            val directories = filesDirectoriesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Process each directory in turn
            directories.forEach { dir ->
                processDirectory(dir)
            }

        } catch (e: Exception) {
            logger.error("Failed to compute and store ATR: ${e.message}", e)
        }
    }

    /**
     * Processes a single directory:
     *   - lists all CSV files
     *   - parses them
     *   - calculates ATR
     *   - saves to DB
     */
    private fun processDirectory(directory: String) {
        val filesPath = Paths.get(directory)

        if (!filesPath.toFile().exists()) {
            logger.error("MQL5 Files directory not found: $directory absolute path: ${filesPath.absolute()}")
            return
        }

        if (!filesPath.isDirectory()) {
            logger.error("MQL5 Files directory is not a directory: $directory absolute path: ${filesPath.absolute()}")
            return
        }

        val csvFiles =
            filesPath.toFile().listFiles { file -> file.isFile && file.name.endsWith(".csv", ignoreCase = true) }
                ?: emptyArray()

        if (csvFiles.isEmpty()) {
            logger.error("No CSV files found in directory: $directory")
            return
        }

        for (file in csvFiles) {
            val symbol = file.nameWithoutExtension
            logger.info("Processing file for symbol: $symbol (in $directory)")

            val symbolDecimalShiftMap = mapOf(
                "SP500" to 2, "XAUUSD" to 2
            )

            val decimalPointShift = symbolDecimalShiftMap[symbol] ?: run {
                logger.error("Decimal point shift not defined for symbol $symbol. Defaulting to 2.")
                2
            }

            val dailyBars = parseDailyBars(file)
            if (dailyBars.size < atrWindow) {
                logger.warn("Not enough data to compute ATR for symbol $symbol (need at least $atrWindow bars).")
                continue
            }

            val atrValue = calculateRollingAtr(dailyBars, atrWindow)
            if (atrValue == null) {
                logger.error("Could not compute ATR for symbol $symbol.")
                continue
            }

            saveAtrModifier(symbol, atrValue, decimalPointShift)
        }
    }

    private fun parseDailyBars(file: File): List<DailyBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val bars = mutableListOf<DailyBar>()

        try {
            Files.lines(file.toPath()).use { lines ->
                lines.asSequence().forEach { line ->
                    if (line.isBlank() || line.startsWith("Date", ignoreCase = true)) {
                        return@forEach
                    }
                    val parts = line.split(",")
                    if (parts.size != 5) {
                        logger.error(
                            "Invalid line format in ${file.name}: Expected 5 parts but got ${parts.size}. Line: $line"
                        )
                        return@forEach
                    }

                    try {
                        val date = LocalDate.parse(parts[0].trim(), formatter)
                        val open = parts[1].trim().toBigDecimal()
                        val high = parts[2].trim().toBigDecimal()
                        val low = parts[3].trim().toBigDecimal()
                        val close = parts[4].trim().toBigDecimal()

                        bars += DailyBar(date, open, high, low, close)
                    } catch (ex: Exception) {
                        logger.error("Skipping malformed line in ${file.name}: $line")
                    }
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException("Failed to read file: ${file.name}", e)
        }

        bars.sortBy { it.date }
        return bars
    }

    fun calculateRollingAtr(bars: List<DailyBar>, windowSize: Int): BigDecimal? {
        if (bars.size < 2) return null
        val trValues = mutableListOf<BigDecimal>()
        var prevClose = bars.first().close

        for (i in 1 until bars.size) {
            val currentBar = bars[i]
            val tr = trueRange(currentBar.high, currentBar.low, prevClose)
            trValues += tr
            prevClose = currentBar.close
        }

        if (trValues.size < windowSize) return null

        val scale = calculateScale(bars)
        var windowSum = trValues.take(windowSize).reduce { acc, bd -> acc + bd }
        var rollingMean = windowSum.divide(windowSize.toBigDecimal(), scale, RoundingMode.HALF_UP)

        for (i in windowSize until trValues.size) {
            windowSum = windowSum - trValues[i - windowSize] + trValues[i]
            rollingMean = windowSum.divide(windowSize.toBigDecimal(), scale, RoundingMode.HALF_UP)
        }

        return rollingMean
    }

    private fun trueRange(high: BigDecimal, low: BigDecimal, prevClose: BigDecimal): BigDecimal {
        val range1 = high - low
        val range2 = (high - prevClose).abs()
        val range3 = (low - prevClose).abs()
        return maxOf(range1, range2, range3)
    }

    private fun saveAtrModifier(symbol: String, atrValue: BigDecimal, decimalPointShift: Int) {
        val shifted = atrValue.movePointRight(decimalPointShift)
        val bdValue = shifted.setScale(0, RoundingMode.HALF_UP)

        val existing = modifierRepository.findBySymbolAndModifierNameAndType(
            symbol, atrModifierName, atrType
        )
        if (existing != null) {
            val updated = existing.copy(modifierValue = bdValue)
            modifierRepository.save(updated)
            logger.info("Updated ATR Modifier for symbol=$symbol to $bdValue.")
        } else {
            val newModifier = Modifier(
                modifierName = atrModifierName,
                modifierValue = bdValue,
                symbol = symbol,
                type = atrType,
                lastModified = LocalDateTime.now()
            )
            modifierRepository.save(newModifier)
            logger.info("Created new ATR Modifier for symbol=$symbol with $bdValue.")
        }
    }

    private fun calculateScale(bars: List<DailyBar>): Int =
        bars.flatMap { listOf(it.open, it.high, it.low, it.close) }.maxOf { it.scale() }
}
