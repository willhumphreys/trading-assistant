package uk.co.threebugs.mochiwhattotrade3.account

import org.mapstruct.Mapper
import java.nio.file.Path
import java.nio.file.Paths

@Mapper(componentModel = "spring")
abstract class AccountMapper {

    abstract fun toDto(account: Account): AccountDto
    abstract fun toEntity(accountDto: AccountDto): Account

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }

    fun toString(path: Path?): String? {
        return path?.toString()
    }
}
