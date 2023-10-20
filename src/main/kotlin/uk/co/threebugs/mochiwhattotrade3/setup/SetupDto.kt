package uk.co.threebugs.mochiwhattotrade3.setup

import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroupDto

data class SetupDto (
    var id: Int? = null,
    var setupGroup: SetupGroupDto? = null,
    var symbol: String? = null,
    var rank: Int? = null,
    var dayOfWeek: Int? = null,
    var hourOfDay: Int? = null,
    var stop: Int? = null,
    var limit: Int? = null,
    var tickOffset: Int? = null,
    var tradeDuration: Int? = null,
    var outOfTime: Int? = null
)
