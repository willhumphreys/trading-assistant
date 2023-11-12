package uk.co.threebugs.darwinexclient.accountsetupgroups

import org.springframework.data.domain.*
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
            .map { accountSetupGroupsDto: AccountSetupGroupsFileDto ->
                val setupGroups = setupGroupsRepository.findByName(accountSetupGroupsDto.setupGroupName)
                    .orElseThrow { RuntimeException("Failed to find setup group: " + accountSetupGroupsDto.setupGroupName) }
                val account =
                    accountRepository.findByName(accountSetupGroupsDto.metatraderAccount) ?: throw RuntimeException(
                        "Failed to find account: " + accountSetupGroupsDto.metatraderAccount
                    )

                val accountSetupGroups: AccountSetupGroups
                val foundAccountSetupGroups = accountSetupGroupsRepository.findByName(accountSetupGroupsDto.name)
                if (foundAccountSetupGroups != null) {

                    foundAccountSetupGroups.setupGroups = setupGroups
                    foundAccountSetupGroups.account = account

                    accountSetupGroups = foundAccountSetupGroups
                } else {

                    accountSetupGroups = AccountSetupGroups()
                    accountSetupGroups.name = accountSetupGroupsDto.name
                    accountSetupGroups.setupGroups = setupGroups
                    accountSetupGroups.account = account
                }

                val savedAccountSetupGroups = accountSetupGroupsRepository.save(accountSetupGroups)

                updateTradingStancesFrom(accountSetupGroupsDto)


                savedAccountSetupGroups
            }
    }

    fun updateTradingStancesFrom(fileAccountSetupGroups: AccountSetupGroupsFileDto) {
        val dbAccountSetupGroups = accountSetupGroupsRepository.findByName(fileAccountSetupGroups.name)

        val dbTradingStances =
            tradingStanceRepository.findByAccountSetupGroups_Name(fileAccountSetupGroups.name, Sort.by("symbol"))

        val fileSymbols = fileAccountSetupGroups.tradingStances.map { it.symbol }.toSet()

        fileAccountSetupGroups.tradingStances.forEach { fileTS ->
            val dbTS = dbTradingStances.find { it.symbol == fileTS.symbol }

            if (dbTS != null) {
                updateTradingStanceIfDifferent(dbTS, fileTS)
            } else {
                tradingStanceRepository.save(
                    TradingStance(
                        symbol = fileTS.symbol,
                        direction = fileTS.direction,
                        accountSetupGroups = dbAccountSetupGroups
                    )
                )
            }
        }

        dbTradingStances.filter { it.symbol !in fileSymbols }.forEach {
            tradingStanceRepository.delete(it)
        }
    }

    private fun updateTradingStanceIfDifferent(dbTS: TradingStance, fileTS: TradingStanceFileDto) {
        var isChanged = false

        if (dbTS.symbol != fileTS.symbol) {
            dbTS.symbol = fileTS.symbol
            isChanged = true
        }
        if (dbTS.direction != fileTS.direction) {
            dbTS.direction = fileTS.direction
            isChanged = true
        }
        // Compare and update other fields as needed

        if (isChanged) {
            tradingStanceRepository.save(dbTS)
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
