package uk.co.threebugs.darwinexclient.setupgroup

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.co.threebugs.darwinexclient.config.AwsConfig
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroups
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsRepository
import java.util.Optional

@Service
class S3SetupGroupService(
    private val s3Client: S3Client,
    private val awsConfig: AwsConfig,
    private val setupGroupRepository: SetupGroupRepository,
    private val setupGroupsRepository: SetupGroupsRepository
) {
    private val logger = LoggerFactory.getLogger(S3SetupGroupService::class.java)

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

        // Find the SetupGroups entity by broker name
        val setupGroups = getOrCreateSetupGroups()

        // Get symbols from S3
        val s3Symbols = getSymbolsFromS3()
        logger.info("Found ${s3Symbols.size} symbols in S3: $s3Symbols")

        // Get existing SetupGroups
        val existingSetupGroups = setupGroupRepository.findBySetupGroups(setupGroups)
        val existingSymbols = existingSetupGroups.mapNotNull { it.symbol }.toSet()
        logger.info("Found ${existingSymbols.size} existing symbols in database: $existingSymbols")

        // Find symbols that exist in S3 but not in the database
        val newSymbols = s3Symbols.filter { it !in existingSymbols }
        logger.info("Found ${newSymbols.size} new symbols to add: $newSymbols")

        // Create SetupGroups for new symbols
        var createdCount = 0
        for (symbol in newSymbols) {
            val setupGroup = SetupGroup(
                setupGroups = setupGroups,
                path = "brokers/${brokerName}/symbols/${symbol}",
                symbol = symbol,
                enabled = true,
                direction = Direction.BOTH // Default direction
            )

            setupGroupRepository.save(setupGroup)
            createdCount++
            logger.info("Created new SetupGroup for symbol: $symbol")
        }

        return createdCount
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
    private fun getSymbolsFromS3(): Set<String> {
        val prefix = "brokers/${brokerName}/symbols/"
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .delimiter("/")
            .build()

        try {
            val response = s3Client.listObjectsV2(request)

            // Extract symbol names from common prefixes
            return response.commonPrefixes().mapNotNull { prefix ->
                val path = prefix.prefix()
                // Format is "brokers/brokerName/symbols/SYMBOLNAME/"
                path.substringAfter("symbols/").trimEnd('/')
            }.toSet()
        } catch (e: Exception) {
            logger.error("Failed to retrieve symbols from S3", e)
            return emptySet()
        }
    }

}