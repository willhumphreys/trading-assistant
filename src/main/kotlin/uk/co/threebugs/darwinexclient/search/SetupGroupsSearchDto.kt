package uk.co.threebugs.darwinexclient.search

import java.nio.file.Path


data class SetupGroupsSearchDto(
    var id: Int? = null,
    var name: String? = null,
    var scriptsDirectory: Path? = null
)

