package uk.co.threebugs.darwinexclient.tradingstance

import org.mapstruct.*
import java.nio.file.*

@Mapper(componentModel = "spring")
abstract class TradingStanceMapper {

    abstract fun toEntity(tradingStanceDto: TradingStanceDto): TradingStance

    @Mapping(target = "id", source = "tradingStance.id")
    @Mapping(target = "symbol", source = "tradingStanceDto.symbol")
    @Mapping(target = "direction", source = "tradingStanceDto.direction")
    @Mapping(target = "accountSetupGroups", source = "tradingStanceDto.accountSetupGroups")
    abstract fun toEntity(tradingStanceDto: TradingStanceDto, tradingStance: TradingStance): TradingStance
    abstract fun toDto(tradingStance: TradingStance): TradingStanceDto


    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }

}