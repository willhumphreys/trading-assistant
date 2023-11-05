package uk.co.threebugs.darwinexclient.setupgroup

import uk.co.threebugs.darwinexclient.setupgroups.*
import java.nio.file.*


data class SetupGroupDto(
    var id: Int? = null,
    val setupGroups: SetupGroupsDto,
    val path: Path,
    val symbol: String,
    val enabled: Boolean,
    val direction: Direction
)

