package uk.co.threebugs.darwinexclient.trade

import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.setup.Setup
import java.nio.file.Path
import java.time.Clock
import java.time.ZonedDateTime

@Mapper(componentModel = "spring")
abstract class TradeMapper {
    abstract fun toDto(trade: Trade): TradeDto

    @Mapping(target = "id", source = "tradeDto.id")
    @Mapping(target = "createdDateTime", expression = "java( ZonedDateTime.now(clock) )")
    abstract fun toEntity(tradeDto: TradeDto, setup: Setup, @Context clock: Clock): Trade

    @Mapping(target = "createdDateTime", expression = "java( ZonedDateTime.now(clock) )")
    abstract fun toEntity(tradeDto: TradeDto, @Context clock: Clock): Trade

    @Mapping(target = "createdDateTime", expression = "java( ZonedDateTime.now(clock) )")
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

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }
}
