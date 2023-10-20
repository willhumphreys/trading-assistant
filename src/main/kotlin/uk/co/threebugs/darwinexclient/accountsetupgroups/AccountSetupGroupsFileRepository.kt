package uk.co.threebugs.darwinexclient.accountsetupgroups

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import java.nio.file.Path

@Repository
class AccountSetupGroupsFileRepository(private val objectMapper: ObjectMapper) {
    fun load(path: Path): List<AccountSetupGroupsFileDto> {
        return try {
            objectMapper.readValue(path.toFile(), object : TypeReference<List<AccountSetupGroupsFileDto>>() {})
        } catch (e: Exception) {
            throw RuntimeException("Failed to load setup groups from file: $path", e)
        }
    }
}
