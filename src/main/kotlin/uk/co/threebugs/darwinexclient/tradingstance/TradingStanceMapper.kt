package uk.co.threebugs.darwinexclient.tradingstance

import org.mapstruct.*
import java.nio.file.*

@Mapper(componentModel = "spring")
abstract class TradingStanceMapper {

    abstract fun toDto(tradingStance: TradingStance): TradingStanceDto
    abstract fun toEntity(tradingStanceDto: TradingStanceDto): TradingStance


    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }

}