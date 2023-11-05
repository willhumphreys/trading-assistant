package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import io.kotest.assertions.*
import okhttp3.*
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.utils.*

class SetupRestCallHelper {

    companion object {
        private const val HOST = "http://localhost:8081"

        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())


        fun getSetups(
        ): List<SetupDto> {
            val request = Request.Builder()
                .url("$HOST/setups")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body?.string() ?: "Empty Response Body"

                val foundSetups = mapper.readValue<List<SetupDto>>(responseBodyText)
                logger.info("Successfully retrieved trades: $responseBodyText")

                return foundSetups
            }
            logger.info("Failed to retrieve setups: ${response.message}")
            fail("Failed to retrieve setups: ${response.message}")

        }
    }

}