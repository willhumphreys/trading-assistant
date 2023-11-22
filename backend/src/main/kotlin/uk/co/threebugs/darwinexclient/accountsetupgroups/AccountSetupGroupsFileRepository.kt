package uk.co.threebugs.darwinexclient.accountsetupgroups

import com.fasterxml.jackson.core.type.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.stereotype.*
import java.nio.file.*

@Repository
class AccountSetupGroupsFileRepository {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    fun load(path: Path): List<AccountSetupGroupsFileDto> {
        return try {
            mapper.readValue(path.toFile(), object : TypeReference<List<AccountSetupGroupsFileDto>>() {})
        } catch (e: Exception) {
            throw RuntimeException("Failed to load accountSetupGroups from file: $path", e)
        }
    }
}
