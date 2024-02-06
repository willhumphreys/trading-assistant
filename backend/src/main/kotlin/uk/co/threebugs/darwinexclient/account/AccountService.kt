package uk.co.threebugs.darwinexclient.account

import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*

@Transactional
@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val accountMapper: AccountMapper
) {
    fun findByName(name: String): AccountDto? {
        val account = accountRepository.findByName(name)
        return account?.let { accountMapper.toDto(it) }
    }

    fun save(accountDto: AccountDto): AccountDto {
        val savedAccount = accountRepository.save(accountMapper.toEntity(accountDto))
        return accountMapper.toDto(savedAccount)
    }

    fun findAll(): List<AccountDto> {
        return accountRepository.findAll()
            .map { accountMapper.toDto(it) }
    }
}
