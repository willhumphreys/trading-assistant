package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import org.mapstruct.Mapper;

import java.nio.file.Path;

@Mapper(componentModel = "spring")
public interface AccountSetupGroupsMapper {
    AccountSetupGroups toEntity(AccountSetupGroupsDto accountSetupGroupsDto);

    AccountSetupGroupsDto toDto(AccountSetupGroups accountSetupGroups);

    default Path toPath(String path) {
        return Path.of(path);
    }

    default String toString(Path path) {
        return path.toString();
    }
}
