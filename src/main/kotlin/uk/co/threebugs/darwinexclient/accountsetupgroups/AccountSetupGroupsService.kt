package uk.co.threebugs.darwinexclient.accountsetupgroups

import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.setupgroups.*
import java.nio.file.*

@Service
class AccountSetupGroupsService(
        private val accountSetupGroupsFileRepository: AccountSetupGroupsFileRepository,
        private val accountSetupGroupsRepository: AccountSetupGroupsRepository,
        private val setupGroupsRepository: SetupGroupsRepository,
        private val accountRepository: AccountRepository,
        private val accountSetupGroupsMapper: AccountSetupGroupsMapper
) {
    fun loadAccountSetupGroups(path: Path): List<AccountSetupGroups> {
        return accountSetupGroupsFileRepository.load(path)
                .map { accountSetupGroupName: AccountSetupGroupsFileDto ->
                    val setupGroups = setupGroupsRepository.findByName(accountSetupGroupName.setupGroupName)
                            .orElseThrow { RuntimeException("Failed to find setup group: " + accountSetupGroupName.setupGroupName) }
                    val account =
                        accountRepository.findByName(accountSetupGroupName.metatraderAccount) ?: throw RuntimeException(
                            "Failed to find account: " + accountSetupGroupName.metatraderAccount
                        )
                    val accountSetupGroups = AccountSetupGroups()
                    accountSetupGroups.name = accountSetupGroupName.name
                    accountSetupGroups.setupGroups = setupGroups
                    accountSetupGroups.account = account

                    val foundAccountSetupGroups = accountSetupGroupsRepository.findByName(accountSetupGroupName.name)
                    if(foundAccountSetupGroups != null) {

                        foundAccountSetupGroups.setupGroups = setupGroups
                        foundAccountSetupGroups.account = account
                        accountSetupGroupsRepository.save(foundAccountSetupGroups)
                    } else {
                        accountSetupGroupsRepository.save(accountSetupGroups)
                    }
                }

    }

    fun findByName(accountSetupGroupsName: String): AccountSetupGroupsDto? {
        return accountSetupGroupsRepository.findByName(accountSetupGroupsName)?.let {
            accountSetupGroupsMapper.toDto(it)
        }
    }

    fun findAll(): List<AccountSetupGroupsDto> {
        return accountSetupGroupsRepository.findAll()
                .map { accountSetupGroups: AccountSetupGroups -> accountSetupGroupsMapper.toDto(accountSetupGroups) }
    }
}
