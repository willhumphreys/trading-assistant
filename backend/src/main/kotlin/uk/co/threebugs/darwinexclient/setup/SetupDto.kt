package uk.co.threebugs.darwinexclient.setup

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroupDto
import java.time.ZonedDateTime

data class SetupDto(
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    val setupGroup: SetupGroupDto,
    val symbol: String,
    val rank: Int,
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val stop: Int,
    val limit: Int,
    val tickOffset: Int,
    val tradeDuration: Int,
    val outOfTime: Int
) {
    @JsonIgnore
    fun isLong(): Boolean = stop < limit
}
