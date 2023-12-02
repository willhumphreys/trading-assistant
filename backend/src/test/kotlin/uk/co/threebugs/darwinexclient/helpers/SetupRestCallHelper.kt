package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import io.kotest.assertions.*
import okhttp3.*
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.setupgroups.*
import uk.co.threebugs.darwinexclient.utils.*

class SetupRestCallHelper {

    companion object {

        private val client = OkHttpClient()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())


        fun getSetups(
        ): List<SetupDto> {
            val request = Request.Builder()
                .url("${RestCallHelper.host}/setups")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()

                val foundSetups = mapper.readValue<List<SetupDto>>(responseBodyText)
                logger.info("Successfully retrieved trades: $responseBodyText")

                return foundSetups
            }
            logger.info("Failed to retrieve setups: ${response.message}")
            fail("Failed to retrieve setups: ${response.message}")

        }

        fun getSetupGroups(
        ): List<SetupGroupsDto> {
            val request = Request.Builder()
                .url("${RestCallHelper.host}/setupGroups")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()

                val foundSetupGroups = mapper.readValue<List<SetupGroupsDto>>(responseBodyText)
                logger.info("Successfully retrieved setupGroups: $responseBodyText")

                return foundSetupGroups
            }
            logger.info("Failed to retrieve setupGroups: ${response.message}")
            fail("Failed to retrieve setupGroups: ${response.message}")

        }

        fun getSetupGroup(
        ): List<SetupGroupDto> {
            val request = Request.Builder()
                .url("${RestCallHelper.host}/setupGroup")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyText = response.body.string()

                val foundSetupGroups = mapper.readValue<List<SetupGroupDto>>(responseBodyText)
                logger.info("Successfully retrieved setupGroup: $responseBodyText")

                return foundSetupGroups
            }
            logger.info("Failed to retrieve setupGroup: ${response.message}")
            fail("Failed to retrieve setupGroup: ${response.message}")

        }

    }

}