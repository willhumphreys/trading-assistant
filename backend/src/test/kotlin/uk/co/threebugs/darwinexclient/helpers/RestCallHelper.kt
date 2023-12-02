package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import io.kotest.assertions.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.tradingstance.*
import uk.co.threebugs.darwinexclient.utils.*
import java.util.*

private const val HOST = "app.host"
private const val TEST_APPLICATION_PROPERTIES = "test-application.properties"

class RestCallHelper {

    companion object {

        val host: String by lazy {
            Properties().apply {
                load(Thread.currentThread().contextClassLoader.getResourceAsStream(TEST_APPLICATION_PROPERTIES))
            }.getProperty(HOST)
        }


        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        fun startProcessing() {
            val startProcessingRequest = Request.Builder()
                .url("$host/actions/start")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun stopProcessing() {
            val startProcessingRequest = Request.Builder()
                .url("$host/actions/stop")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun deleteTradesFromTestAccount(accountName: String) {

            val request = Request.Builder()
                .url("$host/trades?accountName=$accountName")
                .delete()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()
                val rowsDeleted = responseBodyText.toIntOrNull() ?: "Failed to parse response body to Int"
                logger.info("Successfully deleted $rowsDeleted trades for account: $accountName")
            } else {
                logger.info("Failed to delete trades: ${response.message}")
                fail("Failed to delete trades: ${response.message}")
            }
        }

        fun deleteTradesFromSetupGroupsName(setupGroupsName: String) {

            val request = Request.Builder()
                .url("$host/trades?setupGroupsName=$setupGroupsName")
                .delete()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()
                val rowsDeleted = responseBodyText.toIntOrNull() ?: "Failed to parse response body to Int"
                logger.info("Successfully deleted $rowsDeleted trades for setupGroupsName: $setupGroupsName")
            } else {
                logger.info("Failed to delete trades: ${response.message}")
                fail("Failed to delete trades: ${response.message}")
            }
        }


        fun getTradesWithAccountName(
            accountName: String
        ): List<TradeDto> {
            val request = Request.Builder()
                .url("$host/trades?accountName=$accountName")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()

                val foundTrades = mapper.readValue<List<TradeDto>>(responseBodyText)
                logger.info("Successfully retrieved trades: $responseBodyText")

                return foundTrades
            }
            logger.info("Failed to retrieve trades: ${response.message}")
            fail("Failed to retrieve trades: ${response.message}")

        }

        fun getTradesWithSetupGroupsName(
            setupGroupsName: String
        ): List<TradeDto> {
            val request = Request.Builder()
                .url("$host/trades?setupGroupsName=$setupGroupsName")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()

                val foundTrades = mapper.readValue<List<TradeDto>>(responseBodyText)
                logger.info("Successfully retrieved trades: $responseBodyText")

                return foundTrades
            }
            logger.info("Failed to retrieve trades: ${response.message}")
            fail("Failed to retrieve trades: ${response.message}")

        }

        fun setTradingStance(
            symbol: String,
            direction: Direction,
            accountSetupGroupsName: String
        ): TradingStanceDtoOut {
            val requestAllStances = Request.Builder().url("$host/trading-stances").build()

            val responseAllStances = client.newCall(requestAllStances).execute()

            if (responseAllStances.isSuccessful) {

                val tradingStances = mapper.readValue<RootResponse>(responseAllStances.body.string())

                tradingStances.content.first { it.symbol == symbol && it.accountSetupGroups.name == accountSetupGroupsName }
                    .let { tradingStanceDto ->
                        val request = Request.Builder()
                            .url("$host/actions/update-trading-stance/${tradingStanceDto.id}")
                            .post(
                                mapper.writeValueAsString(
                                    UpdateTradingStanceDto(
                                        symbol,
                                        direction,
                                        tradingStanceDto.accountSetupGroups.name
                                    )
                                )
                                    .toRequestBody("application/json".toMediaTypeOrNull())
                            )
                            .build()

                        val response = client.newCall(request).execute()

                        if (response.isSuccessful) {
                            logger.info("Successfully set trading stance for symbol: $symbol to $direction")
                            val tradingStancesAfterUpdate =
                                mapper.readValue<TradingStanceDtoOut>(response.body.string())

                            return tradingStancesAfterUpdate
                        } else {
                            logger.info("Failed to set trading stance for symbol: $symbol to $direction")
                            fail("Failed to set trading stance for symbol: $symbol to $direction")
                        }
                    }


            } else {
                fail("Failed to retrieve trading stances")
            }
        }
    }

    data class RootResponse(
        val content: List<TradingStanceDtoIn>,
        val pageable: Pageable,
        val totalPages: Int,
        val totalElements: Int,
        val last: Boolean,
        val first: Boolean,
        val size: Int,
        val number: Int,
        val sort: Sort,
        val numberOfElements: Int,
        val empty: Boolean
    )


    data class Pageable(
        val pageNumber: Int,
        val pageSize: Int,
        val sort: Sort,
        val offset: Int,
        val paged: Boolean,
        val unpaged: Boolean
    )

    data class Sort(
        val sorted: Boolean,
        val unsorted: Boolean,
        val empty: Boolean
    )
}
