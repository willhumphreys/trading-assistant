package uk.co.threebugs.darwinexclient.setup.s3

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import org.slf4j.LoggerFactory
import uk.co.threebugs.darwinexclient.setup.SetupDto
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroupDto
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsDto
import uk.co.threebugs.darwinexclient.config.AwsConfig
import uk.co.threebugs.darwinexclient.setupgroup.Direction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.*
import java.util.stream.Collectors

/**
 * Service for retrieving trade setups from S3 storage for a specific broker
 */
@Service
class S3SetupService(
    private val s3Client: S3Client,
    private val awsConfig: AwsConfig
) {
    private val logger = LoggerFactory.getLogger(S3SetupService::class.java)

    // Get the bucket name from config
    private val bucketName: String
        get() = awsConfig.s3BucketName()

    // Get the broker name from config
    private val brokerName: String
        get() = awsConfig.brokerName()

    companion object {
        const val S3_PATH_FORMAT = "brokers/%s/symbols/%s/trades.csv"
        const val DEFAULT_SETUP_GROUP_NAME = "Default"
    }

    /**
     * Retrieves all setups for the configured broker from the S3 bucket
     * @return List of SetupDto objects parsed from S3
     */
    fun getSetups(): List<SetupDto> {
        logger.info("Retrieving all setups for broker: $brokerName from bucket: $bucketName")

        val prefixPath = "brokers/$brokerName/symbols/"
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefixPath)
            .build()

        val response = s3Client.listObjectsV2(request)

        val symbols = response.contents().asSequence()
            .map { it.key() }
            .filter { it.endsWith("/trades.csv") }
            .map { it.removePrefix(prefixPath).split("/")[0] }
            .toSet()

        logger.info("Found ${symbols.size} symbols for broker: $brokerName")

        return symbols.flatMap { symbol ->
            loadSetupsForSymbol(symbol)
        }
    }

    /**
     * Retrieves setups for a specific symbol and setup name
     * @param symbol The trading symbol
     * @param name The setup name
     * @return List of SetupDto objects matching the criteria
     */
    fun findBySymbolAndName(symbol: String, name: String): List<SetupDto> {
        return loadSetupsForSymbol(symbol).filter { it.name == name }
    }

    /**
     * Loads setups for a specific symbol under the configured broker
     * @param symbol The symbol to load setups for
     * @return List of SetupDto objects for the specified symbol
     */
    private fun loadSetupsForSymbol(symbol: String): List<SetupDto> {
        logger.debug("Loading setups for symbol: $symbol for broker: $brokerName")

        val s3Path = String.format(S3_PATH_FORMAT, brokerName, symbol)

        try {
            val request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Path)
                .build()

            val response = s3Client.getObject(request)

            val setups = BufferedReader(InputStreamReader(response)).use { reader ->
                reader.lines()
                    .skip(1) // Skip header row
                    .map { line -> parseSetupFromCsvLine(line, symbol) }
                    .filter(Objects::nonNull) // Java way to filter non-null objects
                    .collect(Collectors.toList<SetupDto>())
            }



            logger.debug("Loaded ${setups.size} setups for symbol: $symbol")
            return setups

        } catch (e: Exception) {
            logger.error("Error loading setups for symbol: $symbol", e)
            return emptyList()
        }
    }

    /**
     * Parses a CSV line into a SetupDto
     * @param line The CSV line from trades.csv
     * @param symbol The symbol name
     * @return SetupDto object or null if the line couldn't be parsed or should be skipped
     */
    private fun parseSetupFromCsvLine(line: String?, symbol: String): SetupDto? {
        if (line.isNullOrBlank()) {
            return null
        }

        try {
            val fields = line.split(",")
            if (fields.size < 8) {
                logger.warn("Invalid CSV line format for symbol $symbol: $line")
                return null
            }

            val name = fields[0].trim()
            val dayOfWeek = fields[1].trim().toIntOrNull() ?: 1
            val hourOfDay = fields[2].trim().toIntOrNull() ?: 0
            val rank = fields[3].trim().toIntOrNull() ?: 1
            val stop = fields[4].trim().toIntOrNull() ?: 0
            val limit = fields[5].trim().toIntOrNull() ?: 0
            val tickOffset = fields[6].trim().toIntOrNull() ?: 0
            val tradeDuration = fields[7].trim().toIntOrNull() ?: 0
            val outOfTime = if (fields.size > 8) fields[8].trim().toIntOrNull() ?: 0 else 0

            // Determine direction based on stop and limit values
            val direction = if (stop < limit) Direction.LONG else Direction.SHORT

            // Create a virtual path for the setup
            val setupPath = Paths.get("S3")

            // Create the setup group
            val setupGroupsDto = SetupGroupsDto(name = DEFAULT_SETUP_GROUP_NAME, scriptsDirectory =  Paths.get("S3"))

            val setupGroup = SetupGroupDto(
                id = null,
                setupGroups = setupGroupsDto,
                path = setupPath,
                symbol = symbol,
                enabled = true,
                direction = direction
            )

            return SetupDto(
                id = null,
                createdDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
                setupGroup = setupGroup,
                symbol = symbol,
                rank = rank,
                dayOfWeek = dayOfWeek,
                hourOfDay = hourOfDay,
                stop = stop,
                limit = limit,
                tickOffset = tickOffset,
                tradeDuration = tradeDuration,
                outOfTime = outOfTime,
                name = name
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse setup from CSV line for symbol $symbol: $line", e)
            return null
        }
    }
}