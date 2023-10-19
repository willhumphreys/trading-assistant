package uk.co.threebugs.mochiwhattotrade3.setupgroup;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mapper(componentModel = "spring")
public interface SetupGroupMapper {

    @Mapping(source = "path", target = "path")
    @Mapping(target = "setupGroups", source = "setupGroups")
    SetupGroupDto toDto(SetupGroup setup);
    @Mapping(source = "setupGroups", target = "setupGroups")
    @Mapping(source = "setupDto.id", target = "id")
    SetupGroup toEntity(SetupGroupDto setupDto, SetupGroups setupGroups);

    default String pathToString(Path path) {
        return path == null ? null : path.toString();
    }

    default Path stringToPath(String path) {
        return path == null ? null : Paths.get(path);
    }


}
