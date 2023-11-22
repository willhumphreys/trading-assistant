package uk.co.threebugs.darwinexclient.setup

import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import java.io.*
import java.nio.file.*
import java.time.*
import java.time.temporal.*
import java.util.stream.*

@Repository
class SetupFileRepository {
    fun readCsv(path: Path, symbol: String, setupGroup: SetupGroup): List<Setup> {
        try {
            Files.lines(path).use { lines ->
                return lines
                    .skip(1) // Skip the header
                    .limit(10) // Limit to 10 rows (not including the header)
                    .map { line: String ->
                        val values = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val rank = values[0].replace("\"", "").trim { it <= ' ' }.toInt()
                        val dayOfWeek = values[2].replace("\"", "").trim { it <= ' ' }.toInt()
                        val hourOfDay = values[3].replace("\"", "").trim { it <= ' ' }.toInt()
                        val stop = values[4].toInt()
                        val limit = values[5].toInt()
                        val tickOffset = values[6].toInt()
                        val duration = values[7].toInt()
                        val outOfTime = values[8].toInt()
                        val setup = Setup()
                        setup.rank = rank
                        setup.dayOfWeek = dayOfWeek
                        setup.hourOfDay = hourOfDay
                        setup.stop = stop
                        setup.limit = limit
                        setup.tickOffset = tickOffset
                        setup.tradeDuration = duration
                        setup.outOfTime = outOfTime
                        setup.symbol = symbol
                        setup.setupGroup = setupGroup
                        setup
                    }
                    .collect(Collectors.toList())
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        fun generateStrategy(rank: Int, symbol: String?, placedDateTime: ZonedDateTime, stop: Int, limit: Int): String {
            return "%d-%s-%d-%s".format(rank, symbol, placedDateTime.toEpochSecond(), getLongShort(stop, limit))
        }

        fun getNextEventTime(dayOfWeek: Int, hourOfDay: Int, clock: Clock): ZonedDateTime {
            val now = ZonedDateTime.now(clock)
            var nextEventTime =
                now.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek))).withHour(hourOfDay)
                    .withMinute(0).withSecond(0).withNano(0)
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
