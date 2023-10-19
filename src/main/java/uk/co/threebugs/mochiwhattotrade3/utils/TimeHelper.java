package uk.co.threebugs.mochiwhattotrade3.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class TimeHelper {

    private static final Logger logger = LogManager.getLogger(TimeHelper.class);

    public long addSecondsToCurrentTime(long hoursToAdd) {
        try {

            var diff = getTimeDifferenceInSeconds(ZoneId.of("Europe/Zurich"), ZoneId.of("UTC"));

            var currentTime = Instant.now();
            return currentTime.plus(hoursToAdd, ChronoUnit.HOURS)
                              .getEpochSecond() + diff;
        } catch (Exception e) {
            logger.error("An error occurred while adding seconds to the current time", e);
            throw e;
        }
    }

    public long getTimeDifferenceInSeconds(ZoneId zoneId1, ZoneId zoneId2) {
        var instant = Instant.now();

        var dateTime1 = instant.atZone(zoneId1);
        var dateTime2 = instant.atZone(zoneId2);

        var offsetInSeconds1 = dateTime1.getOffset()
                                        .getTotalSeconds();
        var offsetInSeconds2 = dateTime2.getOffset()
                                        .getTotalSeconds();

        return offsetInSeconds1 - offsetInSeconds2;
    }
}
