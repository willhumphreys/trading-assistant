package uk.co.threebugs.darwinexclient.metatrader.messages

import org.json.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.*
import uk.co.threebugs.darwinexclient.utils.*
import uk.co.threebugs.darwinexclient.websocket.*

@Service
class MessageService(
    private val actionsService: ActionsService,
    private val messageRepository: MessageRepository,
    private val webSocketController: WebSocketController,
    private val slackClient: SlackClient,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int
) {

    fun checkMessages(accountSetupGroupsName: String) {

        if (actionsService.isRunning()) {

            while (true) {
                Helpers.sleep(sleepDelay)

                val newMessages = messageRepository.getNewMessages(accountSetupGroupsName)
                newMessages.forEach {
                    onMessage(it)
                }
            }
        }
    }

    @Synchronized
    fun onMessage(message: JSONObject) {
        if (message["type"]
            == "ERROR"
        ) logger.info(message["type"].toString() + " | " + message["error_type"] + " | " + message["description"]) else if (message["type"]
            == "INFO"
        ) logger.info(message["type"].toString() + " | " + message["message"])
        slackClient.sendSlackNotification("message: $message")

        webSocketController.sendMessage(WebSocketMessage(id = 0, field = "message", value = "$message"), "/topic/ticks")

    }
}