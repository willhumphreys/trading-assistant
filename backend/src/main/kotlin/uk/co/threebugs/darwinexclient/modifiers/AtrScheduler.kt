package uk.co.threebugs.darwinexclient.scheduler

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import uk.co.threebugs.darwinexclient.modifier.Modifier
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    /**
     * Data class to hold daily candle info for ATR calculation.
     */
    data class DailyBar(
        val dateTime: LocalDateTime,
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
            println("MQL5 Files directory not found: $filesDirectory")
            return
        }

        directory.listFiles { file -> file.isFile && file.name.endsWith(".csv", ignoreCase = true) }
            ?.forEach { file ->
                val symbol = file.nameWithoutExtension
                println("Processing file for symbol: $symbol")

                val dailyBars = parseDailyBars(file)
                if (dailyBars.size < atrWindow) {
                    println("Not enough data to compute ATR for symbol $symbol (need at least $atrWindow bars).")
                    return@forEach
                }

                val atrValue = calculateAtr(dailyBars, atrWindow)
                if (atrValue == null) {
                    println("Could not compute ATR for symbol $symbol.")
                    return@forEach
                }

                saveAtrModifier(symbol, atrValue)
            }
    }

    private fun parseDailyBars(file: File): List<DailyBar> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val bars = mutableListOf<DailyBar>()

        file.forEachLine { line ->
            if (line.isBlank()) return@forEachLine

            val parts = line.split(",")
            if (parts.size != 5) {
                throw IllegalArgumentException("Invalid line format in ${file.name}: Expected 5 parts but got ${parts.size}. Line: $line")
            }


            try {
                val dateTime = LocalDateTime.parse(parts[0].trim(), formatter)
                val open = parts[1].toBigDecimal()
                val high = parts[2].toBigDecimal()
                val low = parts[3].toBigDecimal()
                val close = parts[4].toBigDecimal()

                bars += DailyBar(dateTime, open, high, low, close)
            } catch (ex: Exception) {
                println("Skipping malformed line in ${file.name}: $line")
            }
        }
        return bars.sortedBy { it.dateTime }
    }

    private fun calculateAtr(bars: List<DailyBar>, windowSize: Int): BigDecimal? {
        // We need windowSize + 1 bars to produce 'windowSize' TR values
        if (bars.size < windowSize + 1) {
            return null
        }

        // Take the last (windowSize + 1) bars so we can compute windowSize TR values
        val relevantBars = bars.takeLast(windowSize + 1)

        var previousClose = relevantBars.first().close
        val trueRanges = mutableListOf<BigDecimal>()

        // Loop from the second bar in the slice through the end
        // So if we took 15 bars, we get 14 TR values
        for (i in 1 until relevantBars.size) {
            val currentBar = relevantBars[i]
            val tr = trueRange(
                high = currentBar.high,
                low = currentBar.low,
                prevClose = previousClose
            )
            trueRanges += tr
            previousClose = currentBar.close
        }

        // Dynamically compute a suitable scale based on the data, as in your original code
        val scale = calculateScale(bars)

        // Return the average of these 14 TR values
        return if (trueRanges.isEmpty()) {
            null
        } else {
            trueRanges.reduce(BigDecimal::add)
                .divide(BigDecimal(trueRanges.size), scale, RoundingMode.HALF_UP)
        }
    }

    private fun trueRange(high: BigDecimal, low: BigDecimal, prevClose: BigDecimal): BigDecimal {
        val range1 = high.subtract(low)
        val range2 = high.subtract(prevClose).abs()
        val range3 = low.subtract(prevClose).abs()
        return maxOf(range1, range2, range3)
    }

    private fun saveAtrModifier(symbol: String, atrValue: BigDecimal) {
        val scale = atrValue.scale() // Directly use the scale of the calculated ATR value
        val bdValue = atrValue.setScale(scale, RoundingMode.HALF_UP)

        val existing = modifierRepository.findBySymbolAndModifierNameAndType(
            symbol,
            atrModifierName,
            atrType
        )
        if (existing != null) {
            val updated = existing.copy(
                modifierValue = bdValue
            )
            modifierRepository.save(updated)
            println("Updated ATR Modifier for symbol=$symbol to $bdValue.")
        } else {
            val newModifier = Modifier(
                modifierName = atrModifierName,
                modifierValue = bdValue,
                symbol = symbol,
                type = atrType
            )
            modifierRepository.save(newModifier)
            println("Created new ATR Modifier for symbol=$symbol with $bdValue.")
        }
    }

    private fun calculateScale(bars: List<DailyBar>): Int {
        return bars.flatMap { listOf(it.open, it.high, it.low, it.close) } // Flatten all values
            .maxOf { it.scale() } // Find the maximum scale
    }
}