package uk.co.threebugs.darwinexclient.search

import uk.co.threebugs.darwinexclient.setupgroup.*
import java.nio.file.*


data class SetupGroupSearchDto(
    var id: Int? = null,
    var setupGroups: SetupGroupsSearchDto? = null,
    var path: Path? = null,
    var symbol: String? = null,
    var enabled: Boolean? = null,
    var direction: Direction? = null
)

