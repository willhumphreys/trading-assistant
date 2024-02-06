package uk.co.threebugs.darwinexclient.setup

import org.springframework.data.repository.*
import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*

@Transactional
@Service
class SetupService(private val setupRepository: SetupRepository, private val setupMapper: SetupMapper) {
    fun deleteSetupsByAccountName(name: String): Int {
        return setupRepository.deleteSetupsByAccountName(name)
    }

    fun findByIdOrNull(id: Int): Setup? {
        return setupRepository.findByIdOrNull(id)
    }


    fun findAll(): List<SetupDto> {
        return setupRepository.findAll().stream().map { setupMapper.toDto(it) }.toList()
    }

}

