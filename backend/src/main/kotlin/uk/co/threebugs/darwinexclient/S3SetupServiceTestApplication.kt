package uk.co.threebugs.darwinexclient

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import uk.co.threebugs.darwinexclient.config.AwsConfig
import uk.co.threebugs.darwinexclient.setup.s3.S3SetupService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

@SpringBootApplication
class S3SetupServiceTestApplication {

    private val logger = LoggerFactory.getLogger(S3SetupServiceTestApplication::class.java)

    @Bean
    fun s3SetupServiceRunner(s3Client: S3Client, awsConfig: AwsConfig): CommandLineRunner {
        return CommandLineRunner { args ->
            try {
                val bucketName = awsConfig.s3BucketName()
                val brokerName = awsConfig.brokerName()

                logger.info("Testing S3SetupService with bucket: $bucketName, broker: $brokerName")

                // Create the service
                val s3SetupService = S3SetupService(s3Client, awsConfig)

                // Test retrieving all setups
                logger.info("Retrieving all setups from S3...")
                val allSetups = s3SetupService.getSetups()
                logger.info("Found ${allSetups.size} setups in total")

                // Group setups by symbol for better readability in logs
                val setupsBySymbol = allSetups.groupBy { it.symbol }

                setupsBySymbol.forEach { (symbol, setups) ->
                    logger.info("Symbol: $symbol - ${setups.size} setups")

                    // Get unique setup names for this symbol
                    val setupNames = setups.mapNotNull { it.name }.distinct()

                    // Log summary for each setup name
                    setupNames.forEach { name ->
                        val setupsWithName = setups.filter { it.name == name }
                        logger.info("  Setup name: $name - ${setupsWithName.size} variations")

                        // Show details of the first few setups for this name
                        setupsWithName.take(3).forEach { setup ->
                            val direction = if (setup.isLong()) "LONG" else "SHORT"
                            logger.info("    Day: ${setup.dayOfWeek}, Hour: ${setup.hourOfDay}, " +
                                    "Direction: $direction, " +
                                    "Stop: ${setup.stop}, Limit: ${setup.limit}, " +
                                    "Rank: ${setup.rank}")
                        }

                        if (setupsWithName.size > 3) {
                            logger.info("    ... and ${setupsWithName.size - 3} more")
                        }
                    }
                }

                // Test finding setups for a specific symbol and name
                // Use a symbol and name that you know exists in your S3 bucket
                if (setupsBySymbol.isNotEmpty()) {
                    val testSymbol = setupsBySymbol.keys.first()
                    val testName = setupsBySymbol[testSymbol]?.firstOrNull()?.name ?: "DefaultName"

                    logger.info("\nTesting findBySymbolAndName(\"$testSymbol\", \"$testName\")")
                    val filteredSetups = s3SetupService.findBySymbolAndName(testSymbol, testName)
                    logger.info("Found ${filteredSetups.size} setups for $testSymbol with name $testName")

                    // Show details for each filtered setup
                    filteredSetups.forEach { setup ->
                        val direction = if (setup.isLong()) "LONG" else "SHORT"
                        logger.info("  Day: ${setup.dayOfWeek}, Hour: ${setup.hourOfDay}, " +
                                "Direction: $direction, " +
                                "Stop: ${setup.stop}, Limit: ${setup.limit}, " +
                                "Rank: ${setup.rank}, " +
                                "TickOffset: ${setup.tickOffset}, " +
                                "TradeDuration: ${setup.tradeDuration}, " +
                                "OutOfTime: ${setup.outOfTime}")
                    }
                }

            } catch (e: Exception) {
                logger.error("Error during test: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
}

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "longtest")
    System.setProperty("spring.main.web-application-type", "none")

    runApplication<S3SetupServiceTestApplication>(*args)
}