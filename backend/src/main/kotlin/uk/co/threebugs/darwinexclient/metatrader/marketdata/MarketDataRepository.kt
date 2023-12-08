package uk.co.threebugs.darwinexclient.metatrader.marketdata

import com.fasterxml.jackson.core.type.*
import com.fasterxml.jackson.databind.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.metatrader.*
import uk.co.threebugs.darwinexclient.utils.*
import java.nio.file.*

private const val MARKET_DATA_FILE_NAME = "DWX_Market_Data.json"

@Repository
class MarketDataRepository(
    private val objectMapper: ObjectMapper
) {

    private var lastMarketData: Map<String, CurrencyInfo> = java.util.Map.of()

    fun getMarketDataUpdates(accountSetupGroupsDto: AccountSetupGroupsDto): Map<String, CurrencyInfo> {

        val marketDataPath =
            accountSetupGroupsDto.account.metatraderAdvisorPath.resolve("DWX")
                .resolve(MARKET_DATA_FILE_NAME) ?: throw NoSuchElementException("Key 'pathMarketData' not found")

        if (!Files.exists(marketDataPath)) {
            logger.warn("Market data file does not exist: $marketDataPath")
            return emptyMap()
        }

        val data: Map<String, CurrencyInfo> = runCatching {
            objectMapper.readValue(
                marketDataPath.toFile(),
                object : TypeReference<Map<String, CurrencyInfo>>() {})
        }.getOrElse { throwable ->
            logger.warn("DWX_Market_Data.json locked: ${throwable.message}")
            emptyMap()
        }

        if (data == lastMarketData) {
            return emptyMap()
        }


        val newOrChangedEntries = getNewOrChangedEntries(lastMarketData, data)

        lastMarketData = data

        return newOrChangedEntries

    }

    fun <K, V> getNewOrChangedEntries(old: Map<K, V>, news: Map<K, V>): Map<K, V> {
        val newOrChanged = mutableMapOf<K, V>()

        news.forEach { (key, value) ->
            val originalValue = old[key]
            if (originalValue != value) { // Include only if the value is different or new
                newOrChanged[key] = value
            }
        }

        return newOrChanged
    }
}