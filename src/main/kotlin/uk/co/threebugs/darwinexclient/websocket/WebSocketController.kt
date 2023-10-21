package uk.co.threebugs.darwinexclient.websocket

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.web.util.HtmlUtils


@Controller
class WebSocketController(
    val template: SimpMessagingTemplate,
) {


    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    @Throws(Exception::class)
    fun greeting(message: HelloMessage): Greeting {
        Thread.sleep(1000) // simulated delay
        return Greeting("Hello, " + HtmlUtils.htmlEscape(message.name) + "!")
    }

    @Scheduled(fixedRate = 2000)
    fun fireGreeting() {
        template.convertAndSend("/topic/greetings", Greeting("Fire"))
    }

}
