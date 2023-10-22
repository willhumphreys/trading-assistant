package uk.co.threebugs.darwinexclient.websocket

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.util.HtmlUtils


@Controller
class WebSocketController(
    val template: SimpMessagingTemplate,
) {


    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun greeting(message: HelloMessage): webSocketMessage {

        return webSocketMessage("Hello, " + HtmlUtils.htmlEscape(message.name) + "!")
    }

    fun sendMessage(webSocketMessage: webSocketMessage) {
        template.convertAndSend("/topic/greetings", webSocketMessage)
    }

}
