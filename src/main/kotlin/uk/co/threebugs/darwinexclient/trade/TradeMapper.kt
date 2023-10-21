package uk.co.threebugs.darwinexclient.trade

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.setup.Setup
import java.nio.file.Path
import java.time.ZonedDateTime

@Mapper(componentModel = "spring")
abstract class TradeMapper {
    abstract fun toDto(trade: Trade): TradeDto

    @Mapping(source = "tradeDto.createdDateTime", target = "createdDateTime")
    @Mapping(target = "id", source = "tradeDto.id")
    abstract fun toEntity(tradeDto: TradeDto, setup: Setup): Trade

    @Mapping(source = "tradeDto.createdDateTime", target = "createdDateTime")
    abstract fun toEntity(tradeDto: TradeDto): Trade

    @Mapping(target = "createdDateTime", ignore = true)
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
    abstract fun toEntity(setup: Setup, targetPlaceDateTime: ZonedDateTime, account: Account): Trade
    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }
}
