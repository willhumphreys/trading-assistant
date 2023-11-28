package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.clock.*
import uk.co.threebugs.darwinexclient.utils.*
import java.time.*
import java.time.ZoneOffset.*

class TimeHelper {

    companion object {
        private const val HOST = "http://localhost:8081"
        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())


        fun setClockToSpecificDateTime(dateTime: ZonedDateTime) {
            setTime {
                val now = ZonedDateTime.now(UTC)
                Duration.between(now, dateTime)
            }
        }

        //        fun setTimeToNextMonday() {
//            setTime { getDurationBetweenNowAndNextMonday() }
//        }

//        fun setTimeToNearlyCloseTime(trade: TradeDto) {
//            setTime { getDurationBetweenClientNowAndNearlyCloseTime(trade) }
//        }

        private fun setTime(durationProvider: () -> Duration) {
            val duration = durationProvider()

            val json = mapper.writeValueAsString(TimeChangeRequest(duration = duration.toMillis()))
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val setTimeRequest = Request.Builder()
                .url("$HOST/time")
                .put(body)
                .build()

            val setTimeResponse = client.newCall(setTimeRequest).execute()

            logger.info(setTimeResponse.body.string())

            getTime()
        }


        fun getTime(): ZonedDateTime {
            val getTimeRequest = Request.Builder()
                .url("$HOST/time")
                .build()

            val getTimeResponse = client.newCall(getTimeRequest).execute()

            val responseBodyText = getTimeResponse.body.string()

            val timeDto: TimeDto = mapper.readValue(responseBodyText)

            val instant = Instant.ofEpochMilli(timeDto.time)

            //logger.info("Client time is: $localDateTime")
            return ZonedDateTime.ofInstant(instant, UTC)
        }

//        private fun getDurationBetweenNowAndNextMonday(): Duration {
//
//            val nextMondayAt859 = getNextMondayAt859()
//            val now = ZonedDateTime.now(UTC)
//            return Duration.between(now, nextMondayAt859)
//            //return Clock.offset(Clock.system(ZoneOffset.UTC), durationUntilNextMondayAt859)
//
//        }

//        private fun getNextMondayAt859(): ZonedDateTime {
//            val today = ZonedDateTime.now(UTC)
//            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(8).withMinute(59).withSecond(40)
//                .withNano(0)
//        }

//        fun getNextMondayAt9(): ZonedDateTime {
//            return ZonedDateTime.now(UTC).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(9).withMinute(0)
//                .withSecond(0).withNano(0)
//        }
//        private fun getDurationBetweenClientNowAndNearlyCloseTime(trade: TradeDto): Duration {
//
//            val clientTime = getTime()
//            val durationBetweenNowAndClientTime = Duration.between(
//                ZonedDateTime.now(UTC),
//                clientTime
//            )
//
//            logger.info("Duration between now and client time: $durationBetweenNowAndClientTime")
//
//            val durationBetweenClientTimeAndTradeCloseTime = Duration.between(
//                clientTime,
//                trade.targetPlaceDateTime!!.plusHours(trade.setup!!.tradeDuration!!.toLong()).minusSeconds(10)
//            )
//
//            logger.info("Duration between client time and trade close time: $durationBetweenClientTimeAndTradeCloseTime")
//            val totalDuration = durationBetweenNowAndClientTime.plus(durationBetweenClientTimeAndTradeCloseTime)
//
//            logger.info("Total duration: $totalDuration")
//
//            return totalDuration
//
//        }
    }
}