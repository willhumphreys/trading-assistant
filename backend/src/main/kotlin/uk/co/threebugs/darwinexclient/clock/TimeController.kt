package uk.co.threebugs.darwinexclient.clock

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDateTime

data class TimeChangeRequest(val duration: Long)
data class TimeDto(val time: Long)

@RestController
class TimeController(private val mutableClock: MutableClock) {

    @PutMapping("/time")
    fun setTime(@RequestBody request: TimeChangeRequest): ResponseEntity<String> {
        return try {
            val duration = Duration.ofMillis(request.duration)
            mutableClock.setOffset(duration)
            ResponseEntity.ok("Time updated successfully to: ${LocalDateTime.now(mutableClock)}.")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to update time.")
        }
    }

    @GetMapping("/time")
    fun getTime(): ResponseEntity<TimeDto> {
        return try {
            val time = mutableClock.instant()
            val timeDto = TimeDto(time.toEpochMilli())
            ResponseEntity.ok(timeDto)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TimeDto(0))
        }
    }
}
