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
import java.security.*

@Service
open class OrderFileWatcherService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @param:Value("\${metatrader-directory}") private val directory: String,
    @param:Value("\${orders-topic}") private val ordersTopic: String
) {
    private val logger: Logger = LogManager.getLogger(
        OrderFileWatcherService::class.java
    )
    private val objectMapper: ObjectMapper =
        ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())
    private val fileHashes: MutableMap<String, String> = HashMap()

    @EventListener(ApplicationStartedEvent::class)
    @Async
    open fun watchFileChanges() {
        try {
            FileSystems.getDefault().newWatchService().use { watchService ->
                val path = Paths.get(directory)
                path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
                logger.info("Monitoring directory for changes...")
                while (!Thread.currentThread().isInterrupted) {
                    val key = watchService.take()
                    for (event in key.pollEvents()) {
                        val kind = event.kind()

                        if (kind === StandardWatchEventKinds.ENTRY_MODIFY) {
                            val ev = event as WatchEvent<Path>
                            val fileName = ev.context().toString()

                            if ("DWX_Orders.json" == fileName) {
                                val filePath = path.resolve(fileName)
                                val currentHash = getFileContentHash(filePath)

                                if (currentHash != fileHashes[fileName]) {
                                    fileHashes[fileName] = currentHash
                                    processFile(filePath, fileName)
                                }
                            }
                        }
                    }
                    val valid = key.reset()
                    if (!valid) {
                        break
                    }
                }
            }
        } catch (e: IOException) {
            Thread.currentThread().interrupt()
            logger.error("File monitoring interrupted.", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("File monitoring interrupted.", e)
        }
    }

    @Throws(IOException::class)
    private fun getFileContentHash(filePath: Path): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fileContent = Files.readAllBytes(filePath)
            val encodedHash = digest.digest(fileContent)
            return bytesToHex(encodedHash)
        } catch (e: NoSuchAlgorithmException) {
            logger.error("Algorithm for hashing not found.", e)
            return ""
        }
    }

    private fun processFile(filePath: Path, fileName: String) {
        try {
            Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { reader ->
                val orders =
                    objectMapper.readValue(reader, Orders::class.java)
                logger.info("Deserialized orders: $orders")

                val ordersJson = objectMapper.writeValueAsString(orders)
                kafkaTemplate.send(ordersTopic, ordersJson)
                logger.info("Deserialized orders sent to Kafka topic: $orders")
            }
        } catch (e: IOException) {
            logger.error("Error reading file: $fileName", e)
        }
    }

    companion object {
        private fun bytesToHex(hash: ByteArray): String {
            val hexString = StringBuilder(2 * hash.size)
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        }
    }
}