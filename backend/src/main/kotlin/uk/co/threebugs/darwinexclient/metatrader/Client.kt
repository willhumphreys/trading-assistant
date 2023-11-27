package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.commands.*
import uk.co.threebugs.darwinexclient.metatrader.marketdata.*
import uk.co.threebugs.darwinexclient.metatrader.messages.*
import uk.co.threebugs.darwinexclient.metatrader.openorders.*
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.utils.*
import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.concurrent.*


@Component
class Client(
    private val eventHandler: MarketDataService,
    private val accountService: AccountService,
    @param:Value("\${account-setup-groups-name}") private val accountSetupGroupsName: String,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,
    @param:Value("\${accounts-dir}") private val accounts: String,
    private val setupGroupService: SetupGroupService,
    private val setupRepository: SetupRepository,
    private val setupFileRepository: SetupFileRepository,
    accountSetupGroupsService: AccountSetupGroupsService,
    private val actionsService: ActionsService,
    messageRepository: MessageRepository,
    private val messageService: MessageService,
    private val marketDataRepository: MarketDataRepository,
    commandService: CommandService,
    private val objectMapper: ObjectMapper,
    private val openOrdersRepository: OpenOrdersRepository
) {


    private final val accountSetupGroupsDto: AccountSetupGroupsDto

    private final val accountsPath: Path = Paths.get(accounts)

    init {
        val accountDtos = loadMetaTraderInstalls(accountsPath.resolve("metatrader_dirs.json"))
        val symbols = arrayOf("EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF")

        val setupGroupsPath = Paths.get(accounts, "setup-groups")
        try {
            Files.list(setupGroupsPath).use { paths ->
                paths.forEach { setupsPath: Path ->
                    val setupGroups = setupGroupService.loadSetupsFromFile(setupsPath)
                    Arrays.stream(symbols)
                        .forEach { symbol: String -> loadDataFromCsv(symbol, setupGroups) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val accounts =
            accountSetupGroupsService.loadAccountSetupGroups(accountsPath.resolve("account-setup-groups.json"))
        accountSetupGroupsDto = accountSetupGroupsService.findByName(accountSetupGroupsName)
            ?: throw RuntimeException("Failed to find account setup groups: $accountSetupGroupsName")


        val metaTraderDirPath = accountSetupGroupsDto.account.metatraderAdvisorPath
        val f = metaTraderDirPath.toFile()
        if (!f.exists()) {
            logger.info("ERROR: MetaTraderDirPath does not exist!")
            throw RuntimeException("ERROR: MetaTraderDirPath does not exist! $metaTraderDirPath")
        }

        messageRepository.loadMessages(accountSetupGroupsName)

        thread(name = "openOrdersThread") {

            while (true) {
                Helpers.sleep(sleepDelay)
                if (!actionsService.isRunning())
                    continue

                openOrdersRepository.checkOpenOrders(accountSetupGroupsName)
            }

        }
        thread(name = "checkMessage") { messageService.checkMessages(accountSetupGroupsName) }
        thread(name = "checkMarketData") { checkMarketData() }

        commandService.resetCommandIDs(accountSetupGroupsName)
        openOrdersRepository.loadOrders(accountSetupGroupsName)

        // subscribe to tick data:
        commandService.subscribeSymbols(symbols, accountSetupGroupsName)

        actionsService.startUpComplete()


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


    private fun loadDataFromCsv(symbol: String, setupGroups: List<SetupGroup>): List<Setup> {
        return setupGroups
            .filter { setupGroup: SetupGroup ->
                setupGroup.symbol
                    .equals(symbol, ignoreCase = true)
            } //.filter(SetupGroup::getEnabled)
            .flatMap { setupGroup: SetupGroup ->
                setupFileRepository.readCsv(
                    Path.of(setupGroup.path ?: throw RuntimeException("Path not found")),
                    setupGroup.symbol ?: throw RuntimeException("Symbol not found"),
                    setupGroup
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



    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private fun checkMarketData() {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            val data = marketDataRepository.loadMarketData(accountSetupGroupsName)

            data.forEach { (symbol, newCurrencyInfo) ->

                eventHandler.onTick(symbol, newCurrencyInfo.bid, newCurrencyInfo.ask, accountSetupGroupsDto)

            }
        }
    }
}
