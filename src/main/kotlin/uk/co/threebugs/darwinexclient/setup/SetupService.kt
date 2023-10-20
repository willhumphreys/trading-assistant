package uk.co.threebugs.darwinexclient.setup

import org.springframework.stereotype.Service

@Service
class SetupService(val setupRepository: SetupRepository, val setupMapper: SetupMapper)

