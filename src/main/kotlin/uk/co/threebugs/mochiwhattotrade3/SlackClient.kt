package uk.co.threebugs.mochiwhattotrade3

import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.io.entity.StringEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.co.threebugs.mochiwhattotrade3.utils.logger
import java.io.IOException

@Component
class SlackClient private constructor(@param:Value("\${slack.webhook.url}") private val slackWebhookUrl: String, @param:Value("\${slack.webhook.enabled}") private val slackWebhookEnabled: Boolean) {
    fun sendSlackNotification(message: String) {
        logger.info(message)
        try {
            HttpClients.createDefault().use { httpClient ->
                val request = HttpPost(slackWebhookUrl)
                val params = StringEntity("{\"text\":\"$message\"}")
                request.addHeader("Content-type", "application/json")
                request.entity = params
                val response = httpClient.execute<Any?>(request) { classicHttpResponse: ClassicHttpResponse? -> null }
            }
        } catch (e: IOException) {
            logger.error("Error sending Slack notification", e)
        }
    }
}
