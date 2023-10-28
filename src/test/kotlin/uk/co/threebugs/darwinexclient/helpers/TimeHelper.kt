package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.clock.TimeChangeRequest
import uk.co.threebugs.darwinexclient.clock.TimeDto
import uk.co.threebugs.darwinexclient.trade.TradeDto
import uk.co.threebugs.darwinexclient.utils.logger
import java.time.*
import java.time.temporal.TemporalAdjusters

class TimeHelper {

    companion object {
        private const val HOST = "http://localhost:8081"
        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        fun setTimeToNextMonday() {
            setTime { getDurationBetweenNowAndNextMonday() }
        }

        fun setTimeToNearlyCloseTime(trade: TradeDto) {
            setTime { getDurationBetweenClientNowAndNearlyCloseTime(trade) }
        }

        private fun setTime(durationProvider: () -> Duration) {
            val duration = durationProvider()

            val json = mapper.writeValueAsString(TimeChangeRequest(duration = duration.toMillis()))
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val setTimeRequest = Request.Builder()
                .url("$HOST/time")
                .put(body)
                .build()

            val setTimeResponse = client.newCall(setTimeRequest).execute()

            logger.info(setTimeResponse.body?.string() ?: "Empty Response Body")

            getTime()
        }


        fun getTime(): ZonedDateTime {
            val getTimeRequest = Request.Builder()
                .url("$HOST/time")
                .build()

            val getTimeResponse = client.newCall(getTimeRequest).execute()

            val responseBodyText = getTimeResponse.body?.string() ?: "Empty Response Body"

            val timeDto: TimeDto = mapper.readValue(responseBodyText)

            val instant = Instant.ofEpochMilli(timeDto.time)
            val localDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)

            logger.info("Client time is: $localDateTime")
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

        private fun getDurationBetweenClientNowAndNearlyCloseTime(trade: TradeDto): Duration {

            val clientTime = getTime()
            val durationBetweenNowAndClientTime = Duration.between(
                ZonedDateTime.now(ZoneOffset.UTC),
                clientTime
            )

            logger.info("Duration between now and client time: $durationBetweenNowAndClientTime")

            val durationBetweenClientTimeAndTradeCloseTime = Duration.between(
                clientTime,
                trade.targetPlaceDateTime!!.plusHours(trade.setup!!.tradeDuration!!.toLong()).minusSeconds(10)
            )

            logger.info("Duration between client time and trade close time: $durationBetweenClientTimeAndTradeCloseTime")
            val totalDuration = durationBetweenNowAndClientTime.plus(durationBetweenClientTimeAndTradeCloseTime)

            logger.info("Total duration: $totalDuration")

            return totalDuration

        }
    }
}