package uk.co.threebugs.darwinexclient.setupgroups

import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*

@Transactional
@Service
class SetupGroupsService(
    private val setupGroupsRepository: SetupGroupsRepository,
    private val setupGroupsMapper: SetupGroupsMapper
) {
    fun findAll(): List<SetupGroupsDto> {
        return setupGroupsRepository.findAll()
            .stream()
            .map { setupGroups: SetupGroups -> setupGroupsMapper.toDto(setupGroups) }
            .toList()
    }
}
