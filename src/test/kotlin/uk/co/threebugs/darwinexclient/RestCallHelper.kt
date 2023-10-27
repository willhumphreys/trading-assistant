package uk.co.threebugs.darwinexclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.fail
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.trade.TradeDto
import uk.co.threebugs.darwinexclient.utils.logger

private const val host = "http://localhost:8081"

class RestCallHelper {

    companion object {
        fun startProcessing(client: OkHttpClient) {
            val startProcessingRequest = Request.Builder()
                .url("$host/actions/start")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun stopProcessing(client: OkHttpClient) {
            val startProcessingRequest = Request.Builder()
                .url("$host/actions/stop")
                .post("".toRequestBody())
                .build()

            client.newCall(startProcessingRequest).execute()
        }

        fun deleteTradesFromTestAccount(client: OkHttpClient, accountName: String) {

            val request = Request.Builder()
                .url("$host/trades/byAccountName/$accountName")
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

        fun getTrades(
            accountName: String,
            client: OkHttpClient,
            mapper: ObjectMapper
        ): List<TradeDto> {
            val request = Request.Builder()
                .url("$host/trades/byAccountName/$accountName")
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