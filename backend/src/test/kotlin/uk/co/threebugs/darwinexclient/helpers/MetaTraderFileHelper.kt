package uk.co.threebugs.darwinexclient.helpers

import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import uk.co.threebugs.darwinexclient.metatrader.*
import uk.co.threebugs.darwinexclient.utils.*
import java.io.*
import java.math.*
import java.nio.file.*
import java.nio.file.attribute.*
import java.time.*
import kotlin.random.*

class MetaTraderFileHelper {

    companion object {

        private val marketDataPath = Path.of("test-ea-files/DWX/DWX_Market_Data.json")
        private val ordersFile = File("test-ea-files/DWX/DWX_Orders.json")

        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        private val accountInfo = AccountInfo(
            number = 123456,
            leverage = 100,
            balance = BigDecimal(1000.0),
            freeMargin = BigDecimal(1000.0),
            name = "test",
            currency = "USD",
            equity = BigDecimal(1000.0)
        )

        fun deleteFilesBeforeTest(path: Path, prefix: String, suffix: String) {
            Files.walkFileTree(
                path,
                setOf(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val fileName = file.fileName.toString()
                        if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
                            logger.info("Deleting file: $fileName")
                            Files.delete(file)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        return FileVisitResult.CONTINUE
                    }
                })
        }

        fun writeMarketData(symbol: String) {
            mapper.writeValue(
                marketDataPath.toFile(),
                mapOf(
                    symbol to generateCurrentInfo()
                )
            )
            logger.info("Wrote marketData file: $marketDataPath")
        }

        private const val BASE_VALUE = "1.05"

        private fun generateLastThreeDigits(from: Int, until: Int): String {
            return Random.nextInt(from, until).toString()
        }

        private fun generateCurrentInfo(): CurrencyInfo {


            val ask = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 999)}")
            val bid = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 999)}")
            val last = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 999)}")
            val tickValue = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 999)}")

            return CurrencyInfo(
                ask = ask,
                bid = bid,
                last = last,
                tickValue = tickValue
            )
        }


        fun deleteMarketDataFile() {
            if (Files.exists(marketDataPath)) {
                logger.info("Deleting marketData file: $marketDataPath")
                Files.delete(marketDataPath)
            }
        }

        fun writeEmptyOrders() {
            mapper.writeValue(ordersFile, Orders(accountInfo, emptyMap()))
        }

        fun writeOrdersWithMagic(magicTrade1: Int?, magicTrade2: Int?, symbol: String, type: String) {

            val openTradeOrder = Orders(
                accountInfo,
                mapOf(
                    1L to TradeInfo(
                        magic = magicTrade1!!,
                        lots = BigDecimal("0.01"),
                        symbol = symbol,
                        swap = null,
                        openTime = LocalDateTime.parse("2023-10-30T09:00"),
                        openPrice = BigDecimal("$BASE_VALUE${generateLastThreeDigits(500, 600)}"),
                        stopLoss = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 400)}"),
                        takeProfit = BigDecimal("$BASE_VALUE${generateLastThreeDigits(700, 999)}"),
                        type = type,
                        comment = "test",
                    ),
                    2L to TradeInfo(
                        magic = magicTrade2!!,
                        lots = BigDecimal("0.01"),
                        symbol = symbol,
                        swap = null,
                        openTime = LocalDateTime.parse("2023-10-30T09:00"),
                        openPrice = BigDecimal("$BASE_VALUE${generateLastThreeDigits(500, 600)}"),
                        stopLoss = BigDecimal("$BASE_VALUE${generateLastThreeDigits(100, 400)}"),
                        takeProfit = BigDecimal("$BASE_VALUE${generateLastThreeDigits(700, 999)}"),
                        type = type,
                        comment = "test",
                    )

                )
            )
            mapper.writeValue(ordersFile, openTradeOrder)
        }

        fun readOrdersFile(): Orders {
            return mapper.readValue(ordersFile, Orders::class.java)
        }

        fun writeOrdersFile(orders: Orders) {
            mapper.writeValue(ordersFile, orders)
        }
    }

}