package uk.co.threebugs.darwinexclient.tradingstance

import org.mapstruct.*
import uk.co.threebugs.darwinexclient.trade.*
import java.nio.file.*

@Mapper(componentModel = "spring")
abstract class TradingStanceMapper {

    abstract fun toEntity(tradingStanceDtoIn: TradingStanceDtoIn): TradingStance

    @Mapping(target = "id", source = "tradingStance.id")
    @Mapping(target = "symbol", source = "tradingStanceDtoIn.symbol")
    @Mapping(target = "direction", source = "tradingStanceDtoIn.direction")
    @Mapping(target = "accountSetupGroups", source = "tradingStanceDtoIn.accountSetupGroups")
    abstract fun toEntity(tradingStanceDtoIn: TradingStanceDtoIn, tradingStance: TradingStance): TradingStance
    abstract fun toDto(tradingStance: TradingStance): TradingStanceDtoIn
    abstract fun toDto(tradingStance: TradingStance, trades: List<Trade>): TradingStanceDtoOut


    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }

}