package uk.co.threebugs.darwinexclient.websocket

import org.springframework.context.annotation.*
import org.springframework.messaging.simp.config.*
import org.springframework.web.socket.config.annotation.*


@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {


    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/gs-guide-websocket").setAllowedOrigins(
            "http://localhost:3000",
            "http://192.168.86.240:80",
            "http://trading-assistant.mochi-trading.com",
            "http://trading-assistant-frontend-service:80",
            "http://10.244.0.124:3000"
        )

    }
}
