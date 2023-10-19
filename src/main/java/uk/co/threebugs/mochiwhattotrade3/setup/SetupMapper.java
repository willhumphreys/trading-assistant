package uk.co.threebugs.mochiwhattotrade3.setup;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup;

import java.nio.file.Path;

@Mapper(componentModel = "spring")
public interface SetupMapper {

    @Mapping(target = "setupGroup", source = "setupGroup")
    SetupDto toDto(Setup setup);

    @Mapping(target = "id", source = "setupDto.id")
    @Mapping(target = "symbol", source = "setupDto.symbol")
    Setup toEntity(SetupDto setupDto, SetupGroup setupGroup);


    default Path toPath(String path) {
        return Path.of(path);
    }

    default String toString(Path path) {
        return path.toString();
    }
}
