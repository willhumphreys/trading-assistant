package uk.co.threebugs.mochiwhattotrade3.setup

import org.springframework.stereotype.Service

@Service
class SetupService(val setupRepository: SetupRepository, val setupMapper: SetupMapper)

