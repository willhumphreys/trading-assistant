package uk.co.threebugs.darwinexclient.trade

import org.mapstruct.*
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.search.TradeSearchDto
import uk.co.threebugs.darwinexclient.setup.Setup
import java.nio.file.Path
import java.time.Clock
import java.time.ZonedDateTime


@Mapper(componentModel = "spring")
abstract class TradeMapper {
    abstract fun toDto(trade: Trade): TradeDto

    @Mapping(target = "id", source = "tradeDto.id")
    @Mapping(target = "createdDateTime", source = "tradeDto.createdDateTime")
    abstract fun toEntity(tradeDto: TradeDto, setup: Setup, @Context clock: Clock): Trade

    @AfterMapping
    fun setCreatedDateTime(@MappingTarget trade: Trade, @Context clock: Clock) {
        if (trade.createdDateTime == null) {
            trade.createdDateTime = ZonedDateTime.now(clock)
        }
    }

    @AfterMapping
    fun updateLastUpdatedDateTime(@MappingTarget trade: Trade, @Context clock: Clock) {
        trade.lastUpdatedDateTime = ZonedDateTime.now(clock)
    }

    @Mapping(target = "createdDateTime", source = "tradeDto.createdDateTime")
    abstract fun toEntity(tradeDto: TradeDto, @Context clock: Clock): Trade

    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastUpdatedDateTime", ignore = true)
    @Mapping(target = "metatraderId", ignore = true)
    @Mapping(target = "profit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "placedPrice", ignore = true)
    @Mapping(target = "placedDateTime", ignore = true)
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "filledPrice", ignore = true)
    @Mapping(target = "filledDateTime", ignore = true)
    @Mapping(target = "closedPrice", ignore = true)
    @Mapping(target = "closedDateTime", ignore = true)
    @Mapping(target = "closeType", ignore = true)
    @Mapping(target = "id", ignore = true)
    abstract fun toEntity(
        setup: Setup,
        targetPlaceDateTime: ZonedDateTime,
        account: Account,
        @Context clock: Clock
    ): Trade


    abstract fun toEntity(tradeDto: TradeSearchDto, @Context clock: Clock): Trade

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }
}
