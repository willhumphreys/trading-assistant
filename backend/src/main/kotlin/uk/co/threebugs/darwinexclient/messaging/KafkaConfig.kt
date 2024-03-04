package uk.co.threebugs.darwinexclient.messaging

import com.fasterxml.jackson.databind.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.serialization.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.kafka.annotation.*
import org.springframework.kafka.config.*
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import uk.co.threebugs.darwinexclient.metatrader.*


@Configuration
@EnableKafka
class KafkaConfig {


}
