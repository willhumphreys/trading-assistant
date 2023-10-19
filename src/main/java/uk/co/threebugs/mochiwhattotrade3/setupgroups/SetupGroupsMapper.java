package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mapper(componentModel = "spring")
public interface SetupGroupsMapper {
    SetupGroupsDto toDto(SetupGroups setupGroups);

    SetupGroups toEntity(SetupGroupsDto setupGroupsDto);

    @Mapping(target = "id", ignore = true)
    SetupGroups toEntity(SetupGroupsFile setupGroupsDto);

    default String pathToString(Path path) {
        return path == null ? null : path.toString();
    }

    default Path stringToPath(String path) {
        return path == null ? null : Paths.get(path);
    }

}
