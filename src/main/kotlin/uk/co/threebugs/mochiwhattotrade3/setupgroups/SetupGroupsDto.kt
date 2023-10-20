package uk.co.threebugs.mochiwhattotrade3.setupgroups

import java.nio.file.Path


data class SetupGroupsDto(
    var id: Int? = null,
    var name: String? = null,
    var scriptsDirectory: Path? = null
)

