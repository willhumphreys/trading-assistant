package uk.co.threebugs.darwinexclient.setupgroup

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.co.threebugs.darwinexclient.config.AwsConfig
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroups
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsRepository

@Service
class S3SetupGroupService(
    private val s3Client: S3Client,
    private val awsConfig: AwsConfig,
    private val setupGroupRepository: SetupGroupRepository,
    private val setupGroupsRepository: SetupGroupsRepository
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
    fun updateSetupGroupsFromS3(): Int {
        logger.info("Updating SetupGroups for broker: $brokerName")

        val setupGroups = getOrCreateSetupGroups()

        // Get symbols from S3
        val s3Symbols = getSymbolsFromS3()
        logger.info("Found ${s3Symbols.size} valid directional symbols in S3: $s3Symbols")

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
                path = "$brokerName/${symbolWithDirection.symbol}/setup.json",
                enabled = true
            )
            setupGroupRepository.save(setupGroup)
        }

        return newSymbols.size
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


}