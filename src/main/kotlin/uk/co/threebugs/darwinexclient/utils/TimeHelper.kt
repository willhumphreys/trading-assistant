package uk.co.threebugs.darwinexclient.utils

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Component
class TimeHelper {
    fun addSecondsToCurrentTime(hoursToAdd: Long): Long {
        return try {
            val diff = getTimeDifferenceInSeconds(ZoneId.of("Europe/Zurich"), ZoneId.of("UTC"))
            val currentTime = Instant.now()
            currentTime.plus(hoursToAdd, ChronoUnit.HOURS)
                    .epochSecond + diff
        } catch (e: Exception) {
            logger.error("An error occurred while adding seconds to the current time", e)
            throw e
        }
    }

    fun getTimeDifferenceInSeconds(zoneId1: ZoneId?, zoneId2: ZoneId?): Long {
        val instant = Instant.now()
        val dateTime1 = instant.atZone(zoneId1)
        val dateTime2 = instant.atZone(zoneId2)
        val offsetInSeconds1 = dateTime1.offset
                .totalSeconds
        val offsetInSeconds2 = dateTime2.offset
                .totalSeconds
        return (offsetInSeconds1 - offsetInSeconds2).toLong()
    }

    companion object {
        private val logger = LogManager.getLogger(TimeHelper::class.java)
    }
}
