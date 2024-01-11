package uk.co.threebugs.darwinexclient

import jakarta.annotation.*
import org.apache.hc.client5.http.classic.methods.*
import org.apache.hc.client5.http.impl.classic.*
import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.io.entity.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.utils.*
import java.io.*

@Component
class SlackClient private constructor(
    @param:Value("\${slack.webhook.url}") private val slackWebhookUrl: String,
    @param:Value("\${slack.webhook.enabled}") private val slackWebhookEnabled: Boolean
) {

    @PostConstruct
    fun logSlackUrl() {
        logger.info("Slack webhook URL: $slackWebhookUrl")
    }

    fun sendSlackNotification(message: String) {
        if (!slackWebhookEnabled) return

        logger.info(message)
        try {
            HttpClients.createDefault().use { httpClient ->
                val request = HttpPost(slackWebhookUrl)
                val params = StringEntity("{\"text\":\"$message\"}")
                request.addHeader("Content-type", "application/json")
                request.entity = params
                httpClient.execute<Any?>(request) { _: ClassicHttpResponse? -> null }
            }
        } catch (e: IOException) {
            logger.error("Error sending Slack notification", e)
        }
    }
}
