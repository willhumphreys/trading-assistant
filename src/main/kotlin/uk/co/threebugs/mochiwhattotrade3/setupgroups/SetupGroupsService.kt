package uk.co.threebugs.mochiwhattotrade3.setupgroups

import org.springframework.stereotype.Service

@Service
class SetupGroupsService (
     private val setupGroupsRepository: SetupGroupsRepository,
     private val setupGroupsMapper: SetupGroupsMapper
)
{
    fun findAll(): List<SetupGroupsDto> {
        return setupGroupsRepository.findAll()
                .stream()
                .map { setupGroups: SetupGroups -> setupGroupsMapper.toDto(setupGroups) }
                .toList()
    }
}
