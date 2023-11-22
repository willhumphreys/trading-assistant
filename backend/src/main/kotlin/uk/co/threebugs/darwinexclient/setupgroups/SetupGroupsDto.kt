package uk.co.threebugs.darwinexclient.setupgroups

import java.nio.file.Path


data class SetupGroupsDto(
    var id: Int? = null,
    val name: String,
    val scriptsDirectory: Path
)

