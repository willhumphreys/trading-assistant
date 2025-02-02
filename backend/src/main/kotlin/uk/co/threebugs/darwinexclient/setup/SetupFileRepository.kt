package uk.co.threebugs.darwinexclient.setup

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Repository
import uk.co.threebugs.darwinexclient.modifier.Modifier
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifierRepository
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.stream.Collectors

@Repository
class SetupFileRepository(
    private val setupModifierRepository: SetupModifierRepository, private val modifierRepository: ModifierRepository
) {

    private val logger: Logger = LogManager.getLogger(
        SetupFileRepository::class.java
    )

    /**
     * Reads a CSV file at [path], parsing each row into a [ParsedSetupWithModifier],
     * which holds a [Setup] plus an optional [Modifier]. We do NOT persist
     * [SetupModifier] here. The caller should:
     *   1) Save `ParsedSetupWithModifier.setup` -> get the assigned ID.
     *   2) If `modifier` is non-null, save `SetupModifier(setupId, modifier.id)`.
     */
    fun readCsv(
        path: Path, symbol: String, setupGroup: SetupGroup, setupLimit: Int
    ): List<ParsedSetupWithModifier> {

        logger.info("Loading setups for setupGroup: ${setupGroup.id} ${setupGroup.symbol} ${setupGroup.direction}  " +
                "setupGroups ${setupGroup.setupGroups!!.id} ${setupGroup.setupGroups!!.name} " +
                "file: $path absolutePath=${path.toAbsolutePath()}")

        if(!path.toFile().exists()) {
            throw RuntimeException("Setup directory does not exist: $path")
        }

        if (setupLimit > MAX_SETUP_LIMIT) {
            throw RuntimeException("setupLimit cannot be greater than $MAX_SETUP_LIMIT")
        }

        try {
            Files.lines(path).use { lines ->
                return lines.skip(1) // Skip CSV header
                    .limit(setupLimit.toLong()) // Limit lines if desired
                    .map { line -> // Process each line
                        try {
                            val values = line.split(",").dropLastWhile { it.isEmpty() }
                            val rank = values[0].removeSurrounding("\"").trim().toInt()
                            val dayOfWeek = values[2].removeSurrounding("\"").trim().toInt()
                            val hourOfDay = values[3].removeSurrounding("\"").trim().toInt()
                            val stop = values[4].toInt()
                            val limit = values[5].toInt()
                            val tickOffset = values[6].toInt()
                            val duration = values[7].toInt()
                            val outOfTime = values[8].toInt()

                            val setup = Setup().apply {
                                this.rank = rank
                                this.dayOfWeek = dayOfWeek
                                this.hourOfDay = hourOfDay
                                this.stop = stop
                                this.limit = limit
                                this.tickOffset = tickOffset
                                this.tradeDuration = duration
                                this.outOfTime = outOfTime
                                this.symbol = symbol
                                this.setupGroup = setupGroup
                            }

                            // Check if we have a 10th column for the modifier name
                            val modifier: Modifier? = if (values.size > 9) {
                                val modifierName = values[9].removeSurrounding("\"").trim()
                                if (modifierName.isNotEmpty()) {
                                    getModifier(symbol, modifierName)
                                } else null
                            } else null

                            // Construct and return ParsedSetupWithModifier
                            ParsedSetupWithModifier(setup, modifier)
                        } catch (e: IllegalArgumentException) {
                            logger.error("Unable to find the modifier for setup: $line", e)
                            null
                        }
                    }.filter { parsedSetupWithModifier -> parsedSetupWithModifier != null } // Filter out null values
                    .map { it!! } // Unwrap the non-null values (safe due to the filter step above)
                    .collect(Collectors.toList())
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Fetches an existing modifier from the DB. If it does not exist,
     * throws an IllegalArgumentException indicating the modifier was not found.
     */
    private fun getModifier(symbol: String, modifierName: String): Modifier {
        return modifierRepository.findBySymbolAndModifierNameAndType(
            symbol, modifierName, "technicalIndicator"
        ) ?: throw IllegalArgumentException(
            "Modifier with name '$modifierName' and symbol '$symbol' not found."
        )
    }

    companion object {
        private const val MAX_SETUP_LIMIT = 20

        fun getNextEventTime(dayOfWeek: Int, hourOfDay: Int, clock: Clock): ZonedDateTime {
            val now = ZonedDateTime.now(clock)
            var nextEventTime =
                now.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek))).withHour(hourOfDay).withMinute(0)
                    .withSecond(0).withNano(0)

            if (nextEventTime.isBefore(now) || nextEventTime.isEqual(now)) {
                nextEventTime = nextEventTime.plusWeeks(1)
            }
            return nextEventTime
        }

        private fun getLongShort(stop: Int, limit: Int): String {
            return if (stop < limit) "LONG" else "SHORT"
        }
    }
}
