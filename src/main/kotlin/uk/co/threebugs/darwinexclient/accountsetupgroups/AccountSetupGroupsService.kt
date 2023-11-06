package uk.co.threebugs.darwinexclient.accountsetupgroups

import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.setupgroups.*
import uk.co.threebugs.darwinexclient.tradingstance.*
import java.nio.file.*

@Service
class AccountSetupGroupsService(
    private val accountSetupGroupsFileRepository: AccountSetupGroupsFileRepository,
    private val accountSetupGroupsRepository: AccountSetupGroupsRepository,
    private val setupGroupsRepository: SetupGroupsRepository,
    private val accountRepository: AccountRepository,
    private val accountSetupGroupsMapper: AccountSetupGroupsMapper,
    private val tradingStanceRepository: TradingStanceRepository
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

                val accountSetupGroups: AccountSetupGroups
                val foundAccountSetupGroups = accountSetupGroupsRepository.findByName(accountSetupGroupName.name)
                if (foundAccountSetupGroups != null) {

                    foundAccountSetupGroups.setupGroups = setupGroups
                    foundAccountSetupGroups.account = account

                    accountSetupGroups = foundAccountSetupGroups
                } else {

                    accountSetupGroups = AccountSetupGroups()
                    accountSetupGroups.name = accountSetupGroupName.name
                    accountSetupGroups.setupGroups = setupGroups
                    accountSetupGroups.account = account
                }

                val savedAccountSetupGroups = accountSetupGroupsRepository.save(accountSetupGroups)

                accountSetupGroupName.tradingStances.map { tradingStanceFileDto ->
                    TradingStance(
                        symbol = tradingStanceFileDto.symbol,
                        accountSetupGroups = accountSetupGroups,
                        direction = tradingStanceFileDto.direction
                    )
                }.map { tradingStance ->

                    val foundTradingStance = tradingStanceRepository.findBySymbol(tradingStance.symbol!!)

                    if (foundTradingStance != null) {
                        foundTradingStance.direction = tradingStance.direction
                        foundTradingStance.accountSetupGroups = tradingStance.accountSetupGroups
                        tradingStanceRepository.save(foundTradingStance)
                    } else {
                        tradingStanceRepository.save(tradingStance)
                    }

                }

                savedAccountSetupGroups
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
