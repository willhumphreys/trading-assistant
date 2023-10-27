package uk.co.threebugs.darwinexclient.setup

import org.springframework.stereotype.Service

@Service
class SetupService(private val setupRepository: SetupRepository, private val setupMapper: SetupMapper) {
    fun deleteSetupsByAccountName(name: String): Int {
        return setupRepository.deleteSetupsByAccountName(name)
    }


}

