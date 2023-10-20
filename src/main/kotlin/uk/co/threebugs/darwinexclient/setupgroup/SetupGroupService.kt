package uk.co.threebugs.darwinexclient.setupgroup

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupFile
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsFile
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsMapper
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsRepository
import uk.co.threebugs.darwinexclient.utils.logger
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

@Service
class SetupGroupService (
     private val setupGroupMapper: SetupGroupMapper,
     private val setupGroupRepository: SetupGroupRepository,
     private val setupGroupsRepository: SetupGroupsRepository,
     private val setupGroupsMapper: SetupGroupsMapper,
     private val objectMapper: ObjectMapper
) {
    fun loadSetupsFromFile(setupsPath: Path): List<SetupGroup> {
        return try {
            val setupGroupsFile = objectMapper.readValue(setupsPath.toFile(), SetupGroupsFile::class.java)
            val scriptsDirectory = Paths.get(setupGroupsFile.scriptsDirectory)
            val setupGroups = setupGroupsRepository.findByName(setupGroupsFile.name)
                    .orElseGet { setupGroupsRepository.save(setupGroupsMapper.toEntity(setupGroupsFile)) }
            setupGroupsFile.setupGroups
                    .stream()
                    .map { setupGroup: SetupGroupFile ->
                        SetupGroupDto(
                                path = scriptsDirectory.resolve(setupGroup.path),
                                symbol = setupGroup.symbol,
                                enabled = setupGroup.enabled
                        )
                    }
                    .map { setupGroupDto: SetupGroupDto ->
                        val optionalSetupGroup = setupGroupRepository.findByPath(setupGroupDto.path
                                .toString())
                        optionalSetupGroup.orElseGet {
                            val setupGroup = setupGroupMapper.toEntity(setupGroupDto, setupGroups)
                            setupGroupRepository.save(setupGroup)
                        }
                    }
                    .collect(Collectors.toList())
        } catch (e: IOException) {
            logger.error("Error loading setups: " + e.message)
            throw RuntimeException(e)
        }
    }
}
