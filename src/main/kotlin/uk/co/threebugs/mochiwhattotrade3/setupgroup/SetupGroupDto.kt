package uk.co.threebugs.mochiwhattotrade3.setupgroup

import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsDto
import java.nio.file.Path


data class SetupGroupDto(
    var id: Int? = null,
    var setupGroups: SetupGroupsDto? = null,
    var path: Path? = null,
    var symbol: String? = null,
    var enabled: Boolean? = null)

