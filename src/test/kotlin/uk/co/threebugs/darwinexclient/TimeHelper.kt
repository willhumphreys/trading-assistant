package uk.co.threebugs.darwinexclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.clock.TimeChangeRequest
import uk.co.threebugs.darwinexclient.clock.TimeDto
import uk.co.threebugs.darwinexclient.utils.logger
import java.time.*
import java.time.temporal.TemporalAdjusters

private const val host = "http://localhost:8081"

class TimeHelper {

    companion object {

        fun setTime(mapper: ObjectMapper, client: OkHttpClient) {
            val json =
                mapper.writeValueAsString(TimeChangeRequest(duration = getDurationBetweenNowAndNextMonday().toMillis()))
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val setTimeRequest = Request.Builder()
                .url("$host/time")
                .put(body)
                .build()

            val setTimeResponse = client.newCall(setTimeRequest).execute()

            getTime(client, mapper)
        }

        fun getTime(client: OkHttpClient, mapper: ObjectMapper): LocalDateTime {
            val getTimeRequest = Request.Builder()
                .url("$host/time")
                .build()


            val getTimeResponse = client.newCall(getTimeRequest).execute()

            val responseBodyText = getTimeResponse.body?.string() ?: "Empty Response Body"

            val timeDto: TimeDto = mapper.readValue(responseBodyText)

            val instant = Instant.ofEpochMilli(timeDto.time)
            val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

            logger.info("Time set to: ${localDateTime}")
            return localDateTime
        }

        private fun getDurationBetweenNowAndNextMonday(): Duration {

            val today = LocalDate.now(ZoneOffset.UTC)
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            val nextMondayAt859 = ZonedDateTime.of(nextMonday.atTime(8, 59, 50), ZoneOffset.UTC)
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            return Duration.between(now, nextMondayAt859)
            //return Clock.offset(Clock.system(ZoneOffset.UTC), durationUntilNextMondayAt859)

        }
    }
}