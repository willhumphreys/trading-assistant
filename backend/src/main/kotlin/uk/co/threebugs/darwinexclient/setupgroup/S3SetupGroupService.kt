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
     *
     * @return Number of new SetupGroups created
     */
    @Transactional
    fun updateSetupsFromS3(): Int {
        logger.info("Updating SetupGroups for broker: $brokerName")

        val setupGroups = getOrCreateSetupGroups()

        // Get symbols from S3
        val s3Symbols = getSymbolsFromS3()
        logger.info("Found ${s3Symbols.size} valid directional symbols in S3: $s3Symbols")

        val newSymbols = saveSetupsOnS3AndNotStoredLocally(setupGroups, s3Symbols)
        deleteSetupGroupsNotOnS3(setupGroups, s3Symbols)

        synchronizeSetups(setupGroups)

        return newSymbols.size
    }

    private fun saveSetupsOnS3AndNotStoredLocally(
        setupGroups: SetupGroups,
        s3Symbols: Set<SymbolWithDirection>
    ): List<SymbolWithDirection> {
        // Get existing SetupGroups
        val existingSymbols = setupGroupRepository.findBySetupGroups(setupGroups)
            .map { it.symbol }
            .toSet()
        logger.info("Found ${existingSymbols.size} existing symbols in database: $existingSymbols")

        // Filter out existing symbols
        val newSymbols = s3Symbols.filter { !existingSymbols.contains(it.symbol) }
        logger.info("Found ${newSymbols.size} new symbols to add: $newSymbols")

        // Create new SetupGroup entities
        newSymbols.forEach { symbolWithDirection ->
            val setupGroup = SetupGroup(
                symbol = symbolWithDirection.symbol,
                direction = symbolWithDirection.direction,
                setupGroups = setupGroups,
                path = mapToPath(symbolWithDirection),
                enabled = true
            )
            setupGroupRepository.save(setupGroup)
        }
        return newSymbols
    }

    private fun mapToPath(symbolWithDirection: SymbolWithDirection) =
        "brokers/$brokerName/symbols/${symbolWithDirection.symbol}-${symbolWithDirection.direction}/trades.csv"

    private fun deleteSetupGroupsNotOnS3(
        setupGroups: SetupGroups,
        s3Symbols: Set<SymbolWithDirection>
    ) {
        // Delete SetupGroups that are not in S3 anymore
        val existingSetupGroups = setupGroupRepository.findBySetupGroups(setupGroups)

        val s3Paths = s3Symbols.map { mapToPath(it) }.toSet()

        existingSetupGroups.forEach { setupGroup ->
            if (setupGroup.path !in s3Paths) {
                // Find all Setups for this SetupGroup
                setupRepository.findAll()
                    .filter { it.setupGroup?.id == setupGroup.id }
                    .forEach { setup ->
                        // Delete associated SetupModifiers first
                        setupModifierRepository.findAll()
                            .filter { it.setupId == setup.id }
                            .forEach { setupModifierRepository.delete(it) }

                        // Delete the Setup
                        setupRepository.delete(setup)
                    }

                // Finally delete the SetupGroup
                setupGroupRepository.delete(setupGroup)
                logger.info("Deleted SetupGroup with path: ${setupGroup.path}")
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
            logger.info("Found existing SetupGroups for broker: $brokerName")
            setupGroupsOpt.get()
        } else {
            logger.info("Creating new SetupGroups for broker: $brokerName")
            val newSetupGroups = SetupGroups(
                name = brokerName,
                scriptsDirectory = "scripts/$brokerName"
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

        val contents = s3Client.listObjectsV2(request).contents()

        logger.info("Found ${contents.size} total objects in S3")

        return contents
            .map { it.key() }
            .also { keys -> logger.debug("Raw S3 keys: ${keys.joinToString()}") }
            .map { key ->
                key.split("/")[3].also { symbol ->
                    logger.debug("Extracted symbol part: '$symbol' from key: '$key'")
                }
            }
            .mapNotNull { symbolPart ->
                parseSymbolWithDirection(symbolPart)?.also { symbol ->
                    logger.info("Successfully parsed symbol: $symbol from: '$symbolPart'")
                } ?: run {
                    logger.warn("Failed to parse symbol from: '$symbolPart'")
                    null
                }
            }
            .toSet()
            .also { symbols ->
                logger.info("Final parsed symbols count: ${symbols.size}")
                logger.info("Final symbols: ${symbols.joinToString()}")
            }
    }

    private fun parseSymbolWithDirection(rawSymbol: String): SymbolWithDirection? {
        return when {
            rawSymbol.endsWith("-long") -> SymbolWithDirection(
                symbol = rawSymbol.removeSuffix("-long"),
                direction = Direction.LONG
            )
            rawSymbol.endsWith("-short") -> SymbolWithDirection(
                symbol = rawSymbol.removeSuffix("-short"),
                direction = Direction.SHORT
            )
            else -> null
        }
    }

    @Transactional
    fun synchronizeSetups(setupGroups: SetupGroups) {
        val s3SetupsBySymbol = getSetupsFromS3()

        // Process each SetupGroup
        setupGroupRepository.findBySetupGroups(setupGroups).forEach { setupGroup ->
            val symbol = setupGroup.symbol ?: return@forEach
            val direction = setupGroup.direction ?: return@forEach

            val s3Setups = s3SetupsBySymbol[SymbolWithDirection(symbol, direction)] ?: emptyList()
            val localSetups = setupRepository.findAll().filter { it.setupGroup?.id == setupGroup.id }

            // Create new setups that exist in S3 but not locally
            s3Setups.forEach { s3Setup ->
                val matchingLocalSetup = localSetups.find { localSetup ->
                    compareSetups(localSetup, s3Setup)
                }

                if (matchingLocalSetup == null) {
                    val newSetup = Setup(
                        setupGroup = setupGroup,
                        symbol = symbol,
                        rank = s3Setup.rank,
                        dayOfWeek = s3Setup.dayOfWeek,
                        hourOfDay = s3Setup.hourOfDay,
                        stop = s3Setup.stop,
                        limit = s3Setup.limit,
                        tickOffset = s3Setup.tickOffset,
                        tradeDuration = s3Setup.tradeDuration,
                        outOfTime = s3Setup.outOfTime
                    )
                    setupRepository.save(newSetup)
                    logger.info("Created new Setup for symbol: $symbol, rank: ${s3Setup.rank}")
                }
            }

            // Delete local setups that don't exist in S3
            localSetups.forEach { localSetup ->
                val matchingS3Setup = s3Setups.find { s3Setup ->
                    compareSetups(localSetup, s3Setup)
                }

                if (matchingS3Setup == null) {
                    // Delete associated SetupModifiers first
                    setupModifierRepository.findAll()
                        .filter { it.setupId == localSetup.id }
                        .forEach { setupModifierRepository.delete(it) }

                    // Then delete the Setup
                    setupRepository.delete(localSetup)
                    logger.info("Deleted Setup for symbol: $symbol, rank: ${localSetup.rank}")
                }
            }
        }
    }

    private data class S3Setup(
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

    private fun compareSetups(localSetup: Setup, s3Setup: S3Setup): Boolean {
        return localSetup.dayOfWeek == s3Setup.dayOfWeek &&
                localSetup.hourOfDay == s3Setup.hourOfDay &&
                localSetup.stop == s3Setup.stop &&
                localSetup.limit == s3Setup.limit &&
                localSetup.tickOffset == s3Setup.tickOffset &&
                localSetup.tradeDuration == s3Setup.tradeDuration &&
                localSetup.outOfTime == s3Setup.outOfTime
    }

    private fun getSetupsFromS3(): Map<SymbolWithDirection, List<S3Setup>> {
        val setups = mutableMapOf<SymbolWithDirection, List<S3Setup>>()
        val s3Symbols = getSymbolsFromS3()

        s3Symbols.forEach { symbolWithDirection ->
            val tradesFilePath = "brokers/$brokerName/symbols/${symbolWithDirection.symbol}-${symbolWithDirection.direction.toString().lowercase()}/trades.csv"

            try {
                val request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(tradesFilePath)
                    .build()

                val response = s3Client.listObjectsV2(request)
                if (response.hasContents()) {
                    val csvContent = s3Client.getObject { req ->
                        req.bucket(bucketName)
                        req.key(tradesFilePath)
                    }.bufferedReader().use { it.readText() }

                    val setupsList = parseCsvContent(csvContent)
                    setups[symbolWithDirection] = setupsList
                }
            } catch (e: Exception) {
                logger.error("Error reading trades.csv for symbol ${symbolWithDirection.symbol}: ${e.message}")
            }
        }

        return setups
    }

    //id,traderid,broker,dayofweek,hourofday,stop,limit,tickoffset,tradeduration,outoftime
    //0,0,darwinex,1,9,0,0,0,0,0
    private fun parseCsvContent(csvContent: String): List<S3Setup> {
        return csvContent.lineSequence()
            .drop(1) // Skip header
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, line ->
                try {
                    val parts = line.split(",")
                    if (parts.size >= 9) {
                        S3Setup(
                            rank = parts[0].toIntOrNull() ?: throw IllegalStateException("Unable to parse rank from: ${parts[0]}"),
                            traderId = parts[1].toIntOrNull() ?: throw IllegalStateException("Unable to parse traderId from: ${parts[1]}"),
                            dayOfWeek = parts[3].toIntOrNull() ?: throw IllegalStateException("Unable to parse dayOfWeek from: ${parts[3]}"),
                            hourOfDay = parts[4].toIntOrNull() ?: throw IllegalStateException("Unable to parse hourOfDay from: ${parts[4]}"),
                            stop = parts[5].toIntOrNull() ?: throw IllegalStateException("Unable to parse stop from: ${parts[5]}"),
                            limit = parts[6].toIntOrNull() ?: throw IllegalStateException("Unable to parse limit from: ${parts[6]}"),
                            tickOffset = parts[7].toIntOrNull() ?: throw IllegalStateException("Unable to parse tickOffset from: ${parts[7]}"),
                            tradeDuration = parts[8].toIntOrNull() ?: throw IllegalStateException("Unable to parse tradeDuration from: ${parts[8]}"),
                            outOfTime = parts[9].toIntOrNull() ?: throw IllegalStateException("Unable to parse outOfTime from: ${parts[9]}")
                        )
                    } else null
                } catch (e: Exception) {
                    logger.error("Error parsing line: $line", e)
                    null
                }
            }
            .toList()
    }


}