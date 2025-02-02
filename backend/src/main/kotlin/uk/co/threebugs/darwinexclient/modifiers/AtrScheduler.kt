package uk.co.threebugs.darwinexclient.modifiers

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.streams.asSequence
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.utils.logger

@Service
class AtrScheduler(
    private val modifierRepository: ModifierRepository,

    @Value("\${app.mql5.files-directory}")
    private val filesDirectory: String,

    @Value("\${app.atr.window:14}")
    private val atrWindow: Int,

    @Value("\${app.atr.type:technicalIndicator}")
    private val atrType: String,

    @Value("\${app.atr.modifierName:ATR}")
    private val atrModifierName: String
) {

    data class DailyBar(
        val date: LocalDate,
        val open: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val close: BigDecimal
    )

    /**
     * Runs once a day at 00:00 (midnight) server time.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    fun computeAndStoreAtr() {
        val directory = Paths.get(filesDirectory).toFile()
        if (!directory.exists() || !directory.isDirectory) {
            logger.error("MQL5 Files directory not found: $filesDirectory")
            return
        }

        // Look for all CSV files in the configured directory
        val csvFiles = directory.listFiles { file -> file.isFile && file.name.endsWith(".csv", ignoreCase = true) }
            ?: emptyArray()

        if (csvFiles.isEmpty()) {
            logger.error("No CSV files found in directory: $filesDirectory")
            return
        }

        for (file in csvFiles) {
            val symbol = file.nameWithoutExtension
            logger.info("Processing file for symbol: $symbol")

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

            saveAtrModifier(symbol, atrValue)
        }
    }

    private fun parseDailyBars(file: File): List<DailyBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val bars = mutableListOf<DailyBar>()

        try {
            Files.lines(file.toPath()).use { lines ->
                // Convert each line, ignoring the header and blanks
                lines.asSequence().forEach { line ->
                    if (line.isBlank() || line.startsWith("Date", ignoreCase = true)) {
                        return@forEach
                    }

                    val parts = line.split(",")
                    if (parts.size != 5) {
                        throw IllegalArgumentException(
                            "Invalid line format in ${file.name}: Expected 5 parts but got ${parts.size}. Line: $line"
                        )
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

        // Sort by ascending date so we can correctly get the 'previous close'
        bars.sortBy { it.date }
        return bars
    }

    /**
     * 1) For each bar from index=1 onward, compute True Range using bar[i] and bar[i-1].
     * 2) Then compute a rolling mean of size `windowSize` over those TR values.
     * 3) Return the final rolling average as the ATR.
     */
    private fun calculateRollingAtr(bars: List<DailyBar>, windowSize: Int): BigDecimal? {
        if (bars.size < 2) return null

        // 1) Compute TR values for each bar from the second bar onward
        val trValues = mutableListOf<BigDecimal>()
        var prevClose = bars.first().close

        for (i in 1 until bars.size) {
            val currentBar = bars[i]
            val tr = trueRange(currentBar.high, currentBar.low, prevClose)
            trValues += tr
            prevClose = currentBar.close
        }

        if (trValues.size < windowSize) return null

        // We'll use the largest decimal scale found in the data
        val scale = calculateScale(bars)

        // 2) Rolling mean: sum the first window, then slide across
        var windowSum = trValues.take(windowSize).reduce { acc, bd -> acc + bd }
        var rollingMean = windowSum.divide(windowSize.toBigDecimal(), scale, RoundingMode.HALF_UP)

        for (i in windowSize until trValues.size) {
            windowSum = windowSum - trValues[i - windowSize] + trValues[i]
            rollingMean = windowSum.divide(windowSize.toBigDecimal(), scale, RoundingMode.HALF_UP)
        }

        // 3) The final rolling mean is our ATR
        return rollingMean
    }

    private fun trueRange(high: BigDecimal, low: BigDecimal, prevClose: BigDecimal): BigDecimal {
        val range1 = high - low
        val range2 = (high - prevClose).abs()
        val range3 = (low - prevClose).abs()
        return maxOf(range1, range2, range3)
    }

    private fun saveAtrModifier(symbol: String, atrValue: BigDecimal) {
        val scale = atrValue.scale()
        val bdValue = atrValue.setScale(scale, RoundingMode.HALF_UP)

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
                type = atrType
            )
            modifierRepository.save(newModifier)
            logger.info("Created new ATR Modifier for symbol=$symbol with $bdValue.")
        }
    }

    /**
     * Determines an appropriate decimal scale based on the largest scale
     * present in any of the (open, high, low, close) fields.
     */
    private fun calculateScale(bars: List<DailyBar>): Int =
        bars.flatMap { listOf(it.open, it.high, it.low, it.close) }
            .maxOf { it.scale() }
}
