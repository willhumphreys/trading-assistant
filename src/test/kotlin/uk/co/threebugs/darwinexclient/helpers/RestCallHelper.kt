package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import io.kotest.assertions.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.trade.*
import uk.co.threebugs.darwinexclient.utils.*

class RestCallHelper {

    companion object {
        private const val HOST = "http://localhost:8081"

        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        fun startProcessing() {
            val startProcessingRequest = Request.Builder()
                .url("$HOST/actions/start")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun stopProcessing() {
            val startProcessingRequest = Request.Builder()
                .url("$HOST/actions/stop")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun deleteTradesFromTestAccount(accountName: String) {

            val request = Request.Builder()
                .url("$HOST/trades?accountName=$accountName")
                .delete()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body?.string() ?: "Empty Response Body"
                val rowsDeleted = responseBodyText.toIntOrNull() ?: "Failed to parse response body to Int"
                logger.info("Successfully deleted $rowsDeleted trades for account: $accountName")
            } else {
                logger.info("Failed to delete trades: ${response.message}")
                fail("Failed to delete trades: ${response.message}")
            }
        }

        fun deleteTradesFromSetupGroupsName(setupGroupsName: String) {

            val request = Request.Builder()
                .url("$HOST/trades?setupGroupsName=$setupGroupsName")
                .delete()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body?.string() ?: "Empty Response Body"
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
                .url("$HOST/trades?accountName=$accountName")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body?.string() ?: "Empty Response Body"

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
                .url("$HOST/trades?setupGroupsName=$setupGroupsName")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body?.string() ?: "Empty Response Body"

                val foundTrades = mapper.readValue<List<TradeDto>>(responseBodyText)
                logger.info("Successfully retrieved trades: $responseBodyText")

                return foundTrades
            }
            logger.info("Failed to retrieve trades: ${response.message}")
            fail("Failed to retrieve trades: ${response.message}")

        }
    }

}