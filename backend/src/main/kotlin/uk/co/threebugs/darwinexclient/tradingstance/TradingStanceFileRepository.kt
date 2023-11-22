package uk.co.threebugs.darwinexclient.tradingstance

import com.fasterxml.jackson.core.type.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.stereotype.*
import java.nio.file.*

@Repository
class TradingStanceFileRepository {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    fun load(path: Path): List<TradingStanceFileDto> {
        return try {
            mapper.readValue(path.toFile(), object : TypeReference<List<TradingStanceFileDto>>() {})
        } catch (e: Exception) {
            throw RuntimeException("Failed to load tradingStances from file: $path", e)
        }
    }

}