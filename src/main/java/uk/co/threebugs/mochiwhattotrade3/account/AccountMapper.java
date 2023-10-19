package uk.co.threebugs.mochiwhattotrade3.account;

import org.mapstruct.Mapper;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountDto toDto(Account account);

    Account toEntity(AccountDto accountDto);

    default String pathToString(Path path) {
        return path == null ? null : path.toString();
    }

    default Path stringToPath(String path) {
        return path == null ? null : Paths.get(path);
    }

}
