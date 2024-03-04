package kafka

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class KafkaApplication

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = SpringApplication.run(KafkaApplication::class.java, *args)

}