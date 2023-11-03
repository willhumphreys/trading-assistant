package uk.co.threebugs.darwinexclient.search

import java.nio.file.Path


data class SetupGroupSearchDto(
    var id: Int? = null,
    var setupGroups: SetupGroupsSearchDto? = null,
    var path: Path? = null,
    var symbol: String? = null,
    var enabled: Boolean? = null
)

