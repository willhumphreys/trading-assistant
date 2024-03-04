package kafka.service

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import kafka.domain.*
import org.apache.logging.log4j.*
import org.springframework.beans.factory.annotation.*
import org.springframework.boot.context.event.*
import org.springframework.context.event.*
import org.springframework.kafka.core.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*
import java.io.*
import java.nio.charset.*
import java.nio.file.*

@Service
class OrderFileWatcherService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @param:Value("\${metatrader-directory}") private val directory: String,
    @param:Value("\${orders-topic}") private val ordersTopic: String
) {

    private val logger = LogManager.getLogger(OrderFileWatcherService::class.java)
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }

    @EventListener(ApplicationStartedEvent::class)
    @Async
    fun watchFileChanges() {
        val watchService = FileSystems.getDefault().newWatchService()
        val path = Paths.get(directory)
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        logger.info("Monitoring directory for changes...")

        while (true) {
            val key: WatchKey
            try {
                key = watchService.take()
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.error("File monitoring interrupted.", ex)
                return
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                // Check for modify events and read the file's contents
                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {


                    @Suppress("UNCHECKED_CAST")
                    val ev = event as WatchEvent<Path>
                    val fileName = ev.context().toString()

                    logger.info("Event kind: $kind. File affected: $fileName.")
                    try {
                        if (fileName == "DWX_Orders.json") {
                            val filePath = path.resolve(fileName)
                            try {
                                Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { reader ->
                                    val orders = objectMapper.readValue(reader, Orders::class.java)
                                    logger.info("Deserialized orders: $orders")

                                    val ordersJson = objectMapper.writeValueAsString(orders)

                                    kafkaTemplate.send(ordersTopic, ordersJson)
                                    logger.info("Deserialized orders sent to Kafka topic: $orders")
                                }
                            } catch (e: IOException) {
                                logger.error("Error reading file: $fileName", e)
                            }
                        }
                    } catch (e: IOException) {
                        logger.error("Error reading file: $fileName", e)
                    }
                }
            }

            val valid = key.reset()
            if (!valid) {
                break
            }
        }
    }
}