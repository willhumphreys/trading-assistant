package uk.co.threebugs.mochiwhattotrade3;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SlackClient {


    private static final Logger logger = LogManager.getLogger(SlackClient.class);
    private final String slackWebhookUrl;
    private final boolean slackWebhookEnabled;

    private SlackClient(@Value("${slack.webhook.url}") String slackWebhookUrl, @Value("${slack.webhook.enabled}") boolean slackWebhookEnabled) {
        this.slackWebhookUrl = slackWebhookUrl;
        this.slackWebhookEnabled = slackWebhookEnabled;
    }


    public void sendSlackNotification(String message) {

        logger.info(message);

        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(slackWebhookUrl);
            var params = new StringEntity("{\"text\":\"" + message + "\"}");
            request.addHeader("Content-type", "application/json");
            request.setEntity(params);
            var response = httpClient.execute(request, classicHttpResponse -> {
                // logger.info("Slack notification sent successfully");
                return null;
            });
        } catch (IOException e) {
            logger.error("Error sending Slack notification", e);
        }
    }

}
