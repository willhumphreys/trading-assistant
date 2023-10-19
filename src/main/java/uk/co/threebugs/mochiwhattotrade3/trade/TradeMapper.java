package uk.co.threebugs.mochiwhattotrade3.trade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.co.threebugs.mochiwhattotrade3.account.Account;
import uk.co.threebugs.mochiwhattotrade3.setup.Setup;

import java.nio.file.Path;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface TradeMapper {

    @Mapping(target = "id", source = "trade.id")
    TradeDto toDto(Trade trade);

    @Mapping(target = "id", source = "tradeDto.id")
    Trade toEntity(TradeDto tradeDto, Setup setup);


    Trade toEntity(TradeDto tradeDto);

    @Mapping(target = "metatraderId", ignore = true)
    @Mapping(target = "profit", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "placedPrice", ignore = true)
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "filledPrice", ignore = true)
    @Mapping(target = "filledDateTime", ignore = true)
    @Mapping(target = "closedPrice", ignore = true)
    @Mapping(target = "closedDateTime", ignore = true)
    @Mapping(target = "closeType", ignore = true)
    @Mapping(target = "id", ignore = true)
    Trade toEntity(Setup setup, ZonedDateTime placedDateTime, Account account);

    default Path toPath(String path) {
        return Path.of(path);
    }

    default String toString(Path path) {
        if (path == null) return null;
        return path.toString();
    }
}
