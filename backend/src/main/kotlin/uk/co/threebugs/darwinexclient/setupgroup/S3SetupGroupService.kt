package uk.co.threebugs.darwinexclient.setupgroup

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.co.threebugs.darwinexclient.config.AwsConfig
import uk.co.threebugs.darwinexclient.setup.Setup
import uk.co.threebugs.darwinexclient.setup.SetupRepository
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroups
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsRepository
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifierRepository

@Service
class S3SetupGroupService(
    private val s3Client: S3Client,
    private val awsConfig: AwsConfig,
    private val setupGroupRepository: SetupGroupRepository,
    private val setupGroupsRepository: SetupGroupsRepository,
    private val setupModifierRepository: SetupModifierRepository,
    private val setupRepository: SetupRepository
) {
    private val logger = LoggerFactory.getLogger(S3SetupGroupService::class.java)

    data class SymbolWithDirection(
        val symbol: String,
        val direction: Direction
    )

    // Get the bucket name from config
    private val bucketName: String
        get() = awsConfig.s3BucketName()

    // Get the broker name from config
    private val brokerName: String
        get() = awsConfig.brokerName()

    /**
     * Updates the SetupGroups based on symbols found in S3.
     * Uses the broker name from configuration to find the SetupGroups.
     * If a symbol exists in S3 but not in the local database, a new SetupGroup is created.
     * If a symbol exists in S3 and locally but is disabled, it will be re-enabled.
     *
     * @return Number of SetupGroups created or re-enabled.
     */
    @Transactional
    fun updateSetupsFromS3(): Int {
        logger.info("Updating SetupGroups for broker: $brokerName")

        val setupGroups = getOrCreateSetupGroups()

        // Get symbols from S3
        val s3Symbols = getSymbolsFromS3()
        logger.info("Found ${s3Symbols.size} valid directional symbols in S3: $s3Symbols")

        // This function now handles creation of new SetupGroups and re-enabling existing ones.
        val processedSymbols = saveSetupsOnS3AndHandleExisting(setupGroups, s3Symbols)
        // This function disables local SetupGroups that are no longer found in S3.
        // Consider renaming disableSetupGroupsNotOnS3 to reflect it "disables" not "deletes"
        disableSetupGroupsNotOnS3(setupGroups, s3Symbols)

        synchronizeSetups(setupGroups)

        return processedSymbols.size
    }

    /**
     * Processes symbols from S3:
     * - If a symbol from S3 does not exist locally for the given SetupGroups, a new SetupGroup is created and enabled.
     * - If a symbol from S3 exists locally but is disabled (enabled == false), it's re-enabled and its path/direction are updated.
     * - If a symbol from S3 exists locally and is already enabled, its path/direction are updated to ensure consistency with S3.
     * Returns a list of SymbolWithDirection for which SetupGroups were newly created or updated (e.g., re-enabled, path/direction changed).
     */
    private fun saveSetupsOnS3AndHandleExisting( // Renamed for clarity
        setupGroups: SetupGroups,
        s3Symbols: Set<SymbolWithDirection>
    ): List<SymbolWithDirection> {
        val actedUponSymbols = mutableListOf<SymbolWithDirection>()

        // Fetch all existing SetupGroup entities for the current SetupGroups
        val localSetupGroups = setupGroupRepository.findBySetupGroups(setupGroups)
        // Create a map for efficient lookup by symbol string
        val localSetupGroupsBySymbol = localSetupGroups.associateBy { it.symbol }

        logger.info("Found ${localSetupGroups.size} existing SetupGroup records locally for SetupGroups: ${setupGroups.name}")

        s3Symbols.forEach { s3SymbolData ->
            val s3DerivedPath = mapToPath(s3SymbolData)
            val existingLocalGroup = localSetupGroupsBySymbol[s3SymbolData.symbol]
            var setupGroupModifiedOrCreated = false

            if (existingLocalGroup != null) {
                // Symbol exists locally
                var modified = false
                if (existingLocalGroup.enabled == false) { // Check if explicitly disabled
                    existingLocalGroup.enabled = true
                    logger.info("Re-enabled existing SetupGroup for symbol: ${s3SymbolData.symbol} (ID: ${existingLocalGroup.id})")
                    modified = true
                }
                // Always ensure path and direction are consistent with S3, as S3 is the source of truth
                if (existingLocalGroup.path != s3DerivedPath) {
                    existingLocalGroup.path = s3DerivedPath
                    logger.info("Updating path for SetupGroup symbol: ${s3SymbolData.symbol} to '$s3DerivedPath'")
                    modified = true
                }
                if (existingLocalGroup.direction != s3SymbolData.direction) {
                    existingLocalGroup.direction = s3SymbolData.direction
                    logger.info("Updating direction for SetupGroup symbol: ${s3SymbolData.symbol} to '${s3SymbolData.direction}'")
                    modified = true
                }

                if (modified) {
                    setupGroupRepository.save(existingLocalGroup)
                    actedUponSymbols.add(s3SymbolData)
                    setupGroupModifiedOrCreated = true
                }
            } else {
                // Symbol from S3 does not exist locally, create a new SetupGroup
                logger.info("Creating new SetupGroup for S3 symbol: ${s3SymbolData.symbol}, direction: ${s3SymbolData.direction}")
                val newSetupGroup = SetupGroup(
                    symbol = s3SymbolData.symbol,
                    direction = s3SymbolData.direction,
                    setupGroups = setupGroups,
                    path = s3DerivedPath,
                    enabled = true // New setups are always enabled
                )
                setupGroupRepository.save(newSetupGroup)
                actedUponSymbols.add(s3SymbolData)
                setupGroupModifiedOrCreated = true
            }
            if (!setupGroupModifiedOrCreated && existingLocalGroup != null) {
                // If it existed, was already enabled, and no path/direction change was needed,
                // it means it's correctly synced regarding these primary attributes.
                // Depending on requirements, you might still want to add s3SymbolData to actedUponSymbols
                // if merely "found and confirmed" is a state to track.
                // For now, actedUponSymbols only includes created or modified ones.
                logger.debug("SetupGroup for symbol ${s3SymbolData.symbol} already exists, is enabled, and matches S3 path/direction.")
            }
        }

        logger.info("${actedUponSymbols.size} SetupGroups were created or updated/re-enabled based on S3 data.")
        return actedUponSymbols
    }

    private fun mapToPath(symbolWithDirection: SymbolWithDirection) =
        // Ensure lowercase direction in path for consistency if it matters for S3
        "brokers/$brokerName/symbols/${symbolWithDirection.symbol}-${symbolWithDirection.direction.toString().lowercase()}/trades.csv"


    private fun disableSetupGroupsNotOnS3(
        setupGroups: SetupGroups,
        s3Symbols: Set<SymbolWithDirection>
    ) {
        // Disables SetupGroups that are present locally but no longer found on S3
        val localSetupGroups = setupGroupRepository.findBySetupGroups(setupGroups)
        // Create a set of symbols (String) from s3Symbols for quick lookup
        val s3SymbolStrings = s3Symbols.map { it.symbol }.toSet()

        localSetupGroups.forEach { localSetupGroup ->
            // Check if the local setup group's symbol is in the set of symbols from S3
            // and ensure it's currently enabled before trying to disable it.
            if (localSetupGroup.symbol !in s3SymbolStrings && localSetupGroup.enabled == true) {
                logger.info("Disabling SetupGroup for symbol: ${localSetupGroup.symbol} (Path: ${localSetupGroup.path}, ID: ${localSetupGroup.id}) as it's no longer found in S3.")
                localSetupGroup.enabled = false
                setupGroupRepository.save(localSetupGroup)

                // Optionally, also disable all associated Setup entities
                val setupsToDisable = setupRepository.findBySetupGroupAndActive(localSetupGroup, true)

                if (setupsToDisable.isNotEmpty()) {
                    logger.info("Disabling ${setupsToDisable.size} active Setups for disabled SetupGroup ID: ${localSetupGroup.id}")
                    setupsToDisable.forEach { setup ->
                        setup.active = false // Assuming Setup has an 'active' field
                        setupRepository.save(setup)
                    }
                }
            }
        }
    }

    /**
     * Finds or creates the SetupGroups entity for the broker
     * @return The SetupGroups entity
     */
    private fun getOrCreateSetupGroups(): SetupGroups {
        val setupGroupsOpt = setupGroupsRepository.findByName(brokerName)

        return if (setupGroupsOpt.isPresent) {
            logger.info("Found existing SetupGroups for broker: $brokerName (ID: ${setupGroupsOpt.get().id})")
            setupGroupsOpt.get()
        } else {
            logger.info("Creating new SetupGroups for broker: $brokerName")
            val newSetupGroups = SetupGroups(
                name = brokerName,
                scriptsDirectory = "scripts/$brokerName"
                // enabled = true // If SetupGroups has an enabled field
            )
            setupGroupsRepository.save(newSetupGroups)
        }
    }

    /**
     * Retrieves all symbols available in S3 for the configured broker
     * @return Set of symbol names
     */
    private fun getSymbolsFromS3(): Set<SymbolWithDirection> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("brokers/$brokerName/symbols/")
            .build()

        logger.debug("Listing S3 objects with prefix: brokers/$brokerName/symbols/")
        val contents = try {
            s3Client.listObjectsV2(request).contents() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to list objects from S3 bucket '$bucketName' with prefix 'brokers/$brokerName/symbols/': ${e.message}", e)
            return emptySet()
        }

        logger.info("Found ${contents.size} total objects/folders under 'symbols/' in S3 for broker '$brokerName'")

        return contents
            .mapNotNull { s3Object -> s3Object.key() }
            .also { keys -> logger.debug("Raw S3 keys under 'symbols/': ${keys.joinToString()}") }
            .mapNotNull { key ->
                // Expected key format: brokers/BROKER/symbols/SYMBOL-DIRECTION/trades.csv or other files
                // We need to extract the SYMBOL-DIRECTION part from the directory structure.
                val prefixToRemove = "brokers/$brokerName/symbols/"
                if (key.startsWith(prefixToRemove)) {
                    val pathAfterSymbols = key.substring(prefixToRemove.length)
                    // The first part of pathAfterSymbols should be SYMBOL-DIRECTION
                    pathAfterSymbols.split('/').firstOrNull()?.takeIf { it.isNotEmpty() }?.also { symbolPart ->
                        logger.debug("Extracted potential symbol-direction part: '$symbolPart' from key: '$key'")
                    }
                } else {
                    logger.warn("S3 key '$key' does not match expected prefix '$prefixToRemove'")
                    null
                }
            } // Remove nulls from unsuccessful extractions
            .distinct() // Each SYMBOL-DIRECTION directory should be processed once
            .mapNotNull { symbolPart ->
                parseSymbolWithDirection(symbolPart)?.also { symbol ->
                    logger.debug("Successfully parsed S3 symbol: $symbol from part: '$symbolPart'")
                } ?: run {
                    // Log is handled in parseSymbolWithDirection if it returns null
                    null
                }
            }
            .toSet()
            .also { symbols ->
                logger.info("Final parsed S3 SymbolWithDirection count: ${symbols.size}")
                if (symbols.isNotEmpty()) logger.info("Final S3 symbols: ${symbols.joinToString()}") else logger.info("No valid symbols found in S3.")
            }
    }

    private fun parseSymbolWithDirection(rawSymbolDirName: String): SymbolWithDirection? {
        // rawSymbolDirName is expected to be like "EURUSD-long" or "GBPUSD-short"
        val lcRawSymbolDirName = rawSymbolDirName.lowercase() // Standardize to lowercase for suffix check
        return when {
            lcRawSymbolDirName.endsWith("-long") -> SymbolWithDirection(
                // Use original case for symbol part, but remove standardized suffix length
                symbol = rawSymbolDirName.substring(0, rawSymbolDirName.length - "-long".length),
                direction = Direction.LONG
            )
            lcRawSymbolDirName.endsWith("-short") -> SymbolWithDirection(
                symbol = rawSymbolDirName.substring(0, rawSymbolDirName.length - "-short".length),
                direction = Direction.SHORT
            )
            else -> {
                logger.warn("Raw symbol directory name '$rawSymbolDirName' does not conform to SYMBOL-direction format (e.g., EURUSD-long).")
                null
            }
        }
    }

    @Transactional
    fun synchronizeSetups(setupGroups: SetupGroups) {
        val s3SetupsBySymbolMap = getSetupsFromS3CsvData()

        // Process each *enabled* SetupGroup
        setupGroupRepository.findBySetupGroups(setupGroups).filter { it.enabled == true }.forEach { setupGroup ->
            val symbol = setupGroup.symbol ?: return@forEach
            val direction = setupGroup.direction ?: return@forEach

            val currentSymbolWithDirection = SymbolWithDirection(symbol, direction)
            val s3SetupsForThisGroup = s3SetupsBySymbolMap[currentSymbolWithDirection] ?: emptyList()
            // Fetch local Setups that are currently active for this SetupGroup
            val localActiveSetups = setupRepository.findBySetupGroupAndActive(setupGroup, true)

            logger.info("Synchronizing Setups for SetupGroup: ${setupGroup.path} (ID: ${setupGroup.id}). Found ${s3SetupsForThisGroup.size} setups in S3 CSV and ${localActiveSetups.size} active setups locally.")

            // Create or re-activate setups that exist in S3 but not locally (or are inactive locally)
            s3SetupsForThisGroup.forEach { s3SetupData ->
                // Find a local setup by comparing all key fields from the CSV
                val matchingLocalSetup = setupRepository.findAll() // Consider a more targeted query
                    .filter { it.setupGroup?.id == setupGroup.id }
                    .find { compareSetups(it, s3SetupData) }

                if (matchingLocalSetup == null) {
                    val newSetup = Setup(
                        setupGroup = setupGroup,
                        symbol = symbol,
                        rank = s3SetupData.rank,
                        dayOfWeek = s3SetupData.dayOfWeek,
                        hourOfDay = s3SetupData.hourOfDay,
                        stop = s3SetupData.stop,
                        limit = s3SetupData.limit,
                        tickOffset = s3SetupData.tickOffset,
                        tradeDuration = s3SetupData.tradeDuration,
                        outOfTime = s3SetupData.outOfTime,
                        active = true
                    )
                    setupRepository.save(newSetup)
                    logger.info("Created new active Setup for ${setupGroup.path}, rank: ${s3SetupData.rank}")
                } else if (matchingLocalSetup.active == false) { // Check for explicitly inactive
                    matchingLocalSetup.active = true
                    // Potentially update other fields of matchingLocalSetup from s3SetupData if they can change
                    setupRepository.save(matchingLocalSetup)
                    logger.info("Re-activated existing Setup for ${setupGroup.path}, rank: ${matchingLocalSetup.rank} (ID: ${matchingLocalSetup.id})")
                }
            }

            // Deactivate local active setups that don't exist in S3 based on comparison
            localActiveSetups.forEach { localSetup ->
                val matchingS3Setup = s3SetupsForThisGroup.find { s3SetupData ->
                    compareSetups(localSetup, s3SetupData)
                }

                if (matchingS3Setup == null) {
                    logger.info("Deactivating Setup for ${setupGroup.path}, rank: ${localSetup.rank} (ID: ${localSetup.id}) as it's no longer in S3 CSV.")
                    localSetup.active = false
                    setupRepository.save(localSetup)
                }
            }
        }
    }

    // Assuming SetupRepository has:
    // fun findBySetupGroupAndActive(setupGroup: SetupGroup, active: Boolean): List<Setup>

    private data class S3SetupCsvData(
        val rank: Int,
        val traderId: Int,
        val dayOfWeek: Int,
        val hourOfDay: Int,
        val stop: Int,
        val limit: Int,
        val tickOffset: Int,
        val tradeDuration: Int,
        val outOfTime: Int
    )

    private fun compareSetups(localSetup: Setup, s3SetupData: S3SetupCsvData): Boolean {
        return localSetup.rank == s3SetupData.rank &&
                localSetup.dayOfWeek == s3SetupData.dayOfWeek &&
                localSetup.hourOfDay == s3SetupData.hourOfDay &&
                localSetup.stop == s3SetupData.stop &&
                localSetup.limit == s3SetupData.limit &&
                localSetup.tickOffset == s3SetupData.tickOffset &&
                localSetup.tradeDuration == s3SetupData.tradeDuration &&
                localSetup.outOfTime == s3SetupData.outOfTime
    }

    private fun getSetupsFromS3CsvData(): Map<SymbolWithDirection, List<S3SetupCsvData>> {
        val setupsMap = mutableMapOf<SymbolWithDirection, MutableList<S3SetupCsvData>>()

        val enabledSetupGroups = setupGroupRepository.findAll().filter {
            it.enabled == true && it.symbol != null && it.direction != null && it.path != null
        }

        logger.info("Fetching S3 CSV data for ${enabledSetupGroups.size} enabled SetupGroups with valid paths.")

        enabledSetupGroups.forEach { setupGroup ->
            // Non-null assertion operator (!!) is safe here due to the filter above
            val symbolWithDirection = SymbolWithDirection(setupGroup.symbol!!, setupGroup.direction!!)
            val tradesFilePath = setupGroup.path!!

            try {
                logger.debug("Attempting to read S3 object: s3://$bucketName/$tradesFilePath")
                val csvContent = s3Client.getObject { req ->
                    req.bucket(bucketName)
                    req.key(tradesFilePath)
                }.bufferedReader().use { it.readText() }

                val setupsList = parseCsvContent(csvContent)
                if (setupsList.isNotEmpty()) {
                    setupsMap.computeIfAbsent(symbolWithDirection) { mutableListOf() }.addAll(setupsList)
                    logger.info("Successfully parsed ${setupsList.size} setups from $tradesFilePath for ${symbolWithDirection.symbol}-${symbolWithDirection.direction}")
                } else {
                    logger.info("No setups parsed from $tradesFilePath (file might be empty, header-only, or content not matching).")
                }
            } catch (e: software.amazon.awssdk.services.s3.model.NoSuchKeyException) {
                logger.warn("S3 object not found for $tradesFilePath. This might be expected for new/empty SetupGroups.")
            }
            catch (e: Exception) {
                logger.error("Error reading or parsing $tradesFilePath for ${symbolWithDirection.symbol}-${symbolWithDirection.direction}: ${e.message}", e)
            }
        }
        return setupsMap
    }

    private fun parseCsvContent(csvContent: String): List<S3SetupCsvData> {
        return csvContent.lineSequence()
            .drop(1) // Skip header
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, line ->
                try {
                    val parts = line.split(",").map { it.trim() }
                    if (parts.size >= 10) {
                        S3SetupCsvData(
                            rank = parts[0].toIntOrNull() ?: throwNumberFormatException("rank", parts[0], index, line),
                            traderId = parts[1].toIntOrNull() ?: throwNumberFormatException("traderId", parts[1], index, line),
                            dayOfWeek = parts[3].toIntOrNull() ?: throwNumberFormatException("dayOfWeek", parts[3], index, line),
                            hourOfDay = parts[4].toIntOrNull() ?: throwNumberFormatException("hourOfDay", parts[4], index, line),
                            stop = parts[5].toIntOrNull() ?: throwNumberFormatException("stop", parts[5], index, line),
                            limit = parts[6].toIntOrNull() ?: throwNumberFormatException("limit", parts[6], index, line),
                            tickOffset = parts[7].toIntOrNull() ?: throwNumberFormatException("tickOffset", parts[7], index, line),
                            tradeDuration = parts[8].toIntOrNull() ?: throwNumberFormatException("tradeDuration", parts[8], index, line),
                            outOfTime = parts[9].toIntOrNull() ?: throwNumberFormatException("outOfTime", parts[9], index, line)
                        )
                    } else {
                        logger.warn("Skipping CSV line ${index + 2} due to insufficient parts (${parts.size}): '$line'")
                        null
                    }
                } catch (e: Exception) { // Catch NumberFormatException explicitly or other specific exceptions
                    logger.error("Error parsing CSV line ${index + 2}: '$line'. Error: ${e.message}", e)
                    null
                }
            }
            .toList()
    }

    private fun throwNumberFormatException(fieldName: String, value: String, index: Int, line: String): Nothing {
        throw NumberFormatException("Unable to parse $fieldName from value: '$value' on CSV line ${index + 2}: '$line'")
    }

}