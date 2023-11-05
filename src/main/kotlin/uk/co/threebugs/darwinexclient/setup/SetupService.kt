package uk.co.threebugs.darwinexclient.setup

import org.springframework.data.repository.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.setupgroups.*

@Service
class SetupService(private val setupRepository: SetupRepository, private val setupMapper: SetupMapper) {
    fun deleteSetupsByAccountName(name: String): Int {
        return setupRepository.deleteSetupsByAccountName(name)
    }

    fun findByIdOrNull(id: Int): Setup? {
        return setupRepository.findByIdOrNull(id)
    }

    fun findEnabledSetups(symbol: String, setupGroups: SetupGroups): List<SetupDto> {
        return setupRepository.findEnabledSetups(symbol, setupGroups).stream().map { setupMapper.toDto(it) }.toList()
    }

    fun findAll(): List<SetupDto> {
        return setupRepository.findAll().stream().map { setupMapper.toDto(it) }.toList()
    }

}

