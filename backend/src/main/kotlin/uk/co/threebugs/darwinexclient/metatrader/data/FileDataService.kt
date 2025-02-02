package uk.co.threebugs.darwinexclient.metatrader.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.account.AccountDto
import uk.co.threebugs.darwinexclient.account.AccountService
import uk.co.threebugs.darwinexclient.account.MetaTraderDir
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroupsDto
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroupsService
import uk.co.threebugs.darwinexclient.modifiers.AtrScheduler
import uk.co.threebugs.darwinexclient.modifiers.ModifierJsonUpdaterService
import uk.co.threebugs.darwinexclient.setup.*
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroupService
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifier
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifierRepository
import uk.co.threebugs.darwinexclient.utils.logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.log

const val MANUAL_SETUP_NAME = "MANUAL"
private const val MODIFIERS_JSON = "modifiers.json"

@Service
class FileDataService(
    private val setupGroupService: SetupGroupService,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val accountService: AccountService,
    private val setupRepository: SetupRepository,
    private val setupFileRepository: SetupFileRepository,
    private val setupModifierRepository: SetupModifierRepository, // <-- needed to save SetupModifier
    private val objectMapper: ObjectMapper,
    private val modifierJsonUpdaterService: ModifierJsonUpdaterService,
    private val atrScheduler: AtrScheduler,
    @Value("\${accounts-dir}") private val accounts: String,
) {

    /**
     * Loads data for the entire system, returning a list of [AccountSetupGroupsDto].
     */
    internal fun loadData(setupLimit: Int): List<AccountSetupGroupsDto> {
        // 1) Get the base path for "accounts"
        val accountsPath: Path = Paths.get(accounts)

        logger.info("Compute and store atr")

        atrScheduler.computeAndStoreAtr()

        logger.info("Update and create modifiers from json")

        // 2) Update or create Modifiers from "modifiers.json"
        modifierJsonUpdaterService.updateModifiersFromJsonFile(accountsPath.resolve(MODIFIERS_JSON))

        // 3) Load any MetaTrader installs from "metatrader_dirs.json"
        loadMetaTraderInstalls(accountsPath.resolve("metatrader_dirs.json"))

        // 4) For each file in "setup-groups" folder, parse SetupGroups and CSV setups
        val setupGroupsPath = accountsPath.resolve("setup-groups")
        try {
            Files.list(setupGroupsPath).use { paths ->
                paths.forEach { setupsPath: Path ->
                    // Parse the JSON "setup groups"
                    val setupGroups = setupGroupService.loadSetupGroupsFromFile(setupsPath)

                    // For each SetupGroup, parse CSV lines & store in DB
                    setupGroups.forEach { setupGroup: SetupGroup ->
                        loadDataFromCsv(
                            symbol = setupGroup.symbol ?: throw RuntimeException("Symbol missing in SetupGroup."),
                            setupGroups = setupGroups,
                            setupLimit = setupLimit,
                            accountsPath = accountsPath,
                        )
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        // 5) After loading the CSV data, load "account-setup-groups.json"
        return accountSetupGroupsService.loadAccountSetupGroups(
            accountsPath.resolve("account-setup-groups.json")
        )
    }

    /**
     * Reads CSV setups for each [SetupGroup], creates them if not in DB, and
     * creates the corresponding SetupModifier once the Setup is persisted.
     */
    private fun loadDataFromCsv(symbol: String, setupGroups: List<SetupGroup>, setupLimit: Int, accountsPath: Path): List<Setup> {
        return setupGroups
            .filter { it.symbol?.equals(symbol, ignoreCase = true) == true }
            .flatMap { setupGroup ->
                // 1) readCsv -> returns List<ParsedSetupWithModifier>
                val parsedSetups = setupFileRepository.readCsv(
                    accountsPath.resolve(setupGroup.path ?: throw RuntimeException("Path missing in SetupGroup.")),
                    setupGroup.symbol ?: throw RuntimeException("Symbol not found in SetupGroup."),
                    setupGroup,
                    setupLimit)

                // 2) For each row, if the Setup does not already exist, save it.
                //    If there's a Modifier, create the SetupModifier AFTER the Setup is persisted.
                val savedSetups = parsedSetups.map { p ->
                    val existing = setupRepository.findBySymbolAndRankAndSetupGroup(
                        symbol,
                        p.setup.rank ?: throw RuntimeException("Setup rank is null"),
                        setupGroup
                    )
                    val finalSetup = existing ?: setupRepository.save(p.setup)

                    // If we have a modifier in p, create a SetupModifier referencing finalSetup.id + p.modifier.id
                    p.modifier?.let { modifier ->
                        setupModifierRepository.save(
                            SetupModifier(
                                setupId = finalSetup.id ?: throw RuntimeException("Setup has no ID after save"),
                                modifierId = modifier.id
                            )
                        )
                    }
                    finalSetup
                }

                // 3) Also create a "manual" setup if missing
                val manualSetup = Setup(
                    setupGroup = setupGroup,
                    symbol = symbol,
                    rank = -1,
                    dayOfWeek = -1,
                    hourOfDay = -1,
                    stop = -1,
                    limit = -1,
                    tickOffset = -1,
                    tradeDuration = -1,
                    outOfTime = -1,
                    name = MANUAL_SETUP_NAME
                )

                val existingManual = setupRepository.findBySymbolAndRankAndSetupGroup(symbol, -1, setupGroup)
                val finalManual = existingManual ?: setupRepository.save(manualSetup)

                savedSetups + finalManual
            }
    }

    /**
     * Reads `metatrader_dirs.json` from disk. Creates or updates Account records.
     */
    private fun loadMetaTraderInstalls(path: Path): List<AccountDto> {
        return try {
            val metaTraderDirs = objectMapper.readValue<List<MetaTraderDir>>(path.toFile())
            metaTraderDirs.map { metaTraderDir ->
                accountService.findByName(metaTraderDir.name)
                    ?.also { logger.info("MetaTraderDir ${metaTraderDir.name} found in DB") }
                    ?: run {
                        logger.info("MetaTraderDir ${metaTraderDir.name} not found in DB; creating.")
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
}
