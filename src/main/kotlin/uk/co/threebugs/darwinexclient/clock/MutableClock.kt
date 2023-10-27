package uk.co.threebugs.darwinexclient.clock

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Component
class MutableClock(var baseClock: Clock = systemUTC()) : Clock() {
    private var offsetDuration: Duration = Duration.ZERO


    fun addToOffset(duration: Duration) {
        this.offsetDuration = this.offsetDuration.plus(duration)
    }

    fun setOffset(offset: Duration) {
        this.offsetDuration = offset
    }

    override fun withZone(zone: ZoneId?): Clock {
        return MutableClock(baseClock.withZone(zone))
    }

    override fun getZone(): ZoneId {
        return baseClock.zone
    }

    override fun instant(): Instant {
        return baseClock.instant().plus(offsetDuration)
    }

}
