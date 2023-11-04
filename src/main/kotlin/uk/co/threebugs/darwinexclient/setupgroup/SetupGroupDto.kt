package uk.co.threebugs.darwinexclient.setupgroup

import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsDto
import java.nio.file.Path


data class SetupGroupDto(
    var id: Int? = null,
    val setupGroups: SetupGroupsDto,
    val path: Path,
    val symbol: String,
    val enabled: Boolean
)

