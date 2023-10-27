package uk.co.threebugs.darwinexclient.metatrader

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("prod")
@Configuration
class ClockConfig {

//    @Bean
//    fun clock() : Clock {
//        return Clock.system(ZoneOffset.UTC)
//    }
}

@Profile("test")
@Configuration
class TestClockConfig {

//    @Bean
//    fun testClock(): Clock {
//        val today = LocalDate.now(ZoneOffset.UTC)
//        val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
//        val nextMondayAt859 = ZonedDateTime.of(nextMonday.atTime(8, 59), ZoneOffset.UTC)
//        val now = ZonedDateTime.now(ZoneOffset.UTC)
//        val durationUntilNextMondayAt859 = Duration.between(now, nextMondayAt859)
//        return Clock.offset(Clock.system(ZoneOffset.UTC), durationUntilNextMondayAt859)
//    }
}
