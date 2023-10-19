package uk.co.threebugs.mochiwhattotrade3.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;


@Repository
public class SetupFileRepository {

    private static final Logger logger = LogManager.getLogger(SetupFileRepository.class);

    public static String generateStrategy(int rank, String symbol, ZonedDateTime placedDateTime, int stop, int limit) {
        return "%d-%s-%d-%s".formatted(rank, symbol, placedDateTime.toEpochSecond(), getLongShort(stop, limit));
    }

    public static ZonedDateTime getNextEventTime(int dayOfWeek, int hourOfDay) {
        var now = ZonedDateTime.now(UTC);

        var nextEventTime = ZonedDateTime.now(UTC).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek))).withHour(hourOfDay).withMinute(0).withSecond(0).withNano(0);

        if (nextEventTime.isBefore(now) || nextEventTime.isEqual(now)) {
            nextEventTime = nextEventTime.plusWeeks(1);
        }
        return nextEventTime;
    }

    public static String getLongShort(int stop, int limit) {
        return stop < limit ? "LONG" : "SHORT";
    }

    public List<Setup> readCsv(Path path, String symbol, SetupGroup setupGroup) {
        try (var lines = Files.lines(path)) {
            return lines
                    .skip(1)  // Skip the header
                    .limit(10)  // Limit to 10 rows (not including the header)
                    .map(line -> {
                        var values = line.split(",");
                        var rank = Integer.parseInt(values[0].replace("\"", "").trim());
                        var dayOfWeek = Integer.parseInt(values[2].replace("\"", "").trim());
                        var hourOfDay = Integer.parseInt(values[3].replace("\"", "").trim());
                        var stop = Integer.parseInt(values[4]);
                        var limit = Integer.parseInt(values[5]);
                        var tickOffset = Integer.parseInt(values[6]);
                        var duration = Integer.parseInt(values[7]);
                        var outOfTime = Integer.parseInt(values[8]);

                        var setup = new Setup();

                        setup.setRank(rank);
                        setup.setDayOfWeek(dayOfWeek);
                        setup.setHourOfDay(hourOfDay);
                        setup.setStop(stop);
                        setup.setLimit(limit);
                        setup.setTickOffset(tickOffset);
                        setup.setTradeDuration(duration);
                        setup.setOutOfTime(outOfTime);
                        setup.setSymbol(symbol);
                        setup.setSetupGroup(setupGroup);

                        return setup;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
