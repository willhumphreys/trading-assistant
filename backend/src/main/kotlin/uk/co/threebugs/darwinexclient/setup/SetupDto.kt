package uk.co.threebugs.darwinexclient.setup

import com.fasterxml.jackson.annotation.*
import uk.co.threebugs.darwinexclient.setupgroup.* // Assuming SetupGroupDto is in this package
import java.time.*

data class SetupDto(
    var id: Int? = null,
    var createdDateTime: ZonedDateTime? = null,
    val setupGroup: SetupGroupDto, // Assuming SetupGroupDto is defined
    val symbol: String,
    val rank: Int,
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val stop: Int,
    val limit: Int,
    val tickOffset: Int,
    val tradeDuration: Int,
    val outOfTime: Int,
    val name: String? = null,
    val active: Boolean = true // Added active field with default true
) {
    @JsonIgnore
    fun isLong(): Boolean = stop < limit

    @JsonIgnore
    fun isShort(): Boolean = !isLong()
}