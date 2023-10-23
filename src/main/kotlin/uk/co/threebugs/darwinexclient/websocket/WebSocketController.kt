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
    @SendTo("/topic/ticks")
    @Throws(Exception::class)
    fun greeting(message: ServerMessage): WebSocketMessage {

        return WebSocketMessage(
            id = 0,
            field = "test",
            value = "Hello, " + HtmlUtils.htmlEscape(message.value)
        )
    }

    fun sendMessage(webSocketMessage: WebSocketMessage, topic: String) {
        template.convertAndSend(topic, webSocketMessage)
    }

}
