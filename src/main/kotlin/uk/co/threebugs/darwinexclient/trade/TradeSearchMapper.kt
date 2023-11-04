package uk.co.threebugs.darwinexclient.trade

import org.mapstruct.Mapper
import uk.co.threebugs.darwinexclient.search.TradeSearchDto
import java.nio.file.Path

@Mapper(componentModel = "spring")
abstract class TradeSearchMapper {

    abstract fun toDto(trade: Trade): TradeSearchDto

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }

}
