package uk.co.threebugs.darwinexclient.setupgroup

import com.fasterxml.jackson.databind.*
import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*
import uk.co.threebugs.darwinexclient.setupgroups.*
import uk.co.threebugs.darwinexclient.utils.*
import java.io.*
import java.nio.file.*
import java.util.stream.*

@Transactional
@Service
class SetupGroupService(
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
                        enabled = setupGroup.enabled,
                        direction = setupGroup.direction,
                        setupGroups = setupGroupsMapper.toDto(setupGroups)
                    )
                }
                .map { setupGroupDto: SetupGroupDto ->
                    setupGroupRepository.findByPathAndSetupGroups(setupGroupDto.path.toString(), setupGroups) ?: run {
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

    fun findAll(): List<SetupGroupDto> {
        return setupGroupRepository.findAll()
            .stream()
            .map { setupGroup: SetupGroup -> setupGroupMapper.toDto(setupGroup) }
            .toList()
    }

    fun findUniqueSymbolsBySetupGroups(setupGroups: SetupGroupsDto): List<String> {
        return setupGroupRepository.findBySetupGroups(setupGroupsMapper.toEntity(setupGroups))
            .stream()
            .map { setupGroup: SetupGroup -> setupGroup.symbol!! }
            .distinct()
            .toList()
    }
}
