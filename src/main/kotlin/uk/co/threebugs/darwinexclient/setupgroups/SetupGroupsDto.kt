package uk.co.threebugs.darwinexclient.setupgroups

import java.nio.file.Path


data class SetupGroupsDto(
    var id: Int? = null,
    var name: String? = null,
    var scriptsDirectory: Path? = null
)

