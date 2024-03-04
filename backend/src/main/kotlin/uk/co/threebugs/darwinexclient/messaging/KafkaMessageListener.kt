package uk.co.threebugs.darwinexclient.messaging

import org.apache.logging.log4j.LogManager
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.metatrader.*

@Service
class KafkaMessageListener {

    private val logger = LogManager.getLogger(KafkaMessageListener::class.java)

    @KafkaListener(topics = ["DWX_Orders"], groupId = "trading-assistant")
    fun listen(orders: Orders) {
        logger.info("Received and deserialized message from Kafka topic: $orders")
    }
}
