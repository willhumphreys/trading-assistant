package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups

import org.mapstruct.Mapper
import java.nio.file.Path

@Mapper(componentModel = "spring")
abstract class AccountSetupGroupsMapper {
    abstract fun toEntity(accountSetupGroupsDto: AccountSetupGroupsDto): AccountSetupGroups
    abstract fun toDto(accountSetupGroups: AccountSetupGroups): AccountSetupGroupsDto
    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }
}
