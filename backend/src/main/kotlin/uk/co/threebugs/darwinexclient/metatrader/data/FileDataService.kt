package uk.co.threebugs.darwinexclient.metatrader.data

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.utils.*
import java.io.*
import java.nio.file.*

@Service
class FileDataService(
    private val setupGroupService: SetupGroupService,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val accountService: AccountService,
    private val setupRepository: SetupRepository,
    private val setupFileRepository: SetupFileRepository,
    private val objectMapper: ObjectMapper,
    @param:Value("\${accounts-dir}") private val accounts: String,
) {


    internal fun loadData(symbols: List<String>, setupLimit: Int): List<AccountSetupGroupsDto> {

        val accountsPath: Path = Paths.get(accounts)

        loadMetaTraderInstalls(accountsPath.resolve("metatrader_dirs.json"))

        val setupGroupsPath = Paths.get(accounts, "setup-groups")
        try {
            Files.list(setupGroupsPath).use { paths ->
                paths.forEach { setupsPath: Path ->
                    val setupGroups = setupGroupService.loadSetupsFromFile(setupsPath)
                    symbols.forEach { symbol: String -> loadDataFromCsv(symbol, setupGroups, setupLimit) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return accountSetupGroupsService.loadAccountSetupGroups(accountsPath.resolve("account-setup-groups.json"))

    }

    private fun loadMetaTraderInstalls(path: Path): List<AccountDto> {
        return try {
            val metaTraderDirs = objectMapper.readValue<List<MetaTraderDir>>(path.toFile())
            metaTraderDirs.map { metaTraderDir ->
                accountService.findByName(metaTraderDir.name)
                    ?.also { logger.info("MetaTraderDir ${metaTraderDir.name} found in database") }
                    ?: run {
                        logger.info("MetaTraderDir ${metaTraderDir.name} not found in database")
                        accountService.save(
                            AccountDto(
                                metatraderAdvisorPath = Path.of(metaTraderDir.dirPath),
                                name = metaTraderDir.name
                            )
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("Error loading MetaTrader directories from JSON file", e)
            throw RuntimeException("Error loading MetaTrader directories from JSON file", e)
        }
    }


    private fun loadDataFromCsv(symbol: String, setupGroups: List<SetupGroup>, setupLimit: Int): List<Setup> {
        return setupGroups
            .filter { setupGroup: SetupGroup ->
                setupGroup.symbol
                    .equals(symbol, ignoreCase = true)
            } //.filter(SetupGroup::getEnabled)
            .flatMap { setupGroup: SetupGroup ->
                setupFileRepository.readCsv(
                    Path.of(setupGroup.path ?: throw RuntimeException("Path not found")),
                    setupGroup.symbol ?: throw RuntimeException("Symbol not found"),
                    setupGroup,
                    setupLimit
                )
            }
            .map { setup: Setup ->
                val setup1 = setupRepository.findBySymbolAndRankAndSetupGroup(symbol, setup.rank!!, setup.setupGroup!!)
                if (setup1 == null) {
                    setupRepository.save(setup)
                }
                setup
            }

    }

}