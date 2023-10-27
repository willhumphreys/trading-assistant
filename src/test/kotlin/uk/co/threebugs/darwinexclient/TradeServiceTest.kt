import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.clock.TimeChangeRequest
import uk.co.threebugs.darwinexclient.clock.TimeDto
import uk.co.threebugs.darwinexclient.metatrader.AccountInfo
import uk.co.threebugs.darwinexclient.metatrader.CurrencyInfo
import uk.co.threebugs.darwinexclient.metatrader.Orders
import uk.co.threebugs.darwinexclient.metatrader.TradeInfo
import uk.co.threebugs.darwinexclient.trade.TradeDto
import uk.co.threebugs.darwinexclient.utils.logger
import java.io.File
import java.math.BigDecimal
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.*
import java.time.temporal.TemporalAdjusters


class TradeServiceTest : FunSpec() {

    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val accountName = "test"

    private val accountInfo = AccountInfo(
        number = 123456,
        leverage = 100,
        balance = BigDecimal(1000.0),
        freeMargin = BigDecimal(1000.0),
        name = "test",
        currency = "USD",
        equity = BigDecimal(1000.0)
    )

    private val ordersFile = File("test-ea-files/DWX/DWX_Orders.json")
    private val ordersStoredFile = File("test-ea-files/DWX/DWX_Orders_Stored.json")
    private val marketDataPath = Path.of("test-ea-files/DWX/DWX_Market_Data.json")

    private val emptyOrders = Orders(accountInfo, emptyMap())

    override suspend fun beforeEach(testCase: TestCase) {
        deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")
        //    deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Orders", ".json")

        mapper.writeValue(ordersFile, emptyOrders)

        //mapper.writeValue(ordersStoredFile, emptyOrders)

        deleteTradesFromTestAccount(client, accountName)
        //deleteSetupsFromTestAccount(client, accountName)

        deleteMarketDataFile(marketDataPath)

        checkTradesIsEmpty(accountName, client, mapper)

        setTime(mapper, client)

        startProcessing(client)

        delay(5000)


        super.beforeEach(testCase)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {

        mapper.writeValue(ordersFile, emptyOrders)
        // mapper.writeValue(ordersStoredFile, emptyOrders)

        delay(5000L)

        stopProcessing(client)

        deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")

        super.afterEach(testCase, result)
    }

    init {

        test("place 2 eurusd long trades") {
            runBlocking {

                writeMarketDataFile(
                    mapper, marketDataPath, "EURUSD", CurrencyInfo(
                        ask = BigDecimal("1.5600"),
                        bid = BigDecimal("1.5620"),
                        last = BigDecimal("1.5600"),
                        tickValue = BigDecimal("1.5600")
                    )
                )

                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {
                        logger.info("Waiting for EA to write file...")
                        logger.info("Client time ${getTime(client, mapper)}")
                        val foundTrades = getTrades(accountName, client, mapper)

                        if (foundTrades.isNotEmpty()) {

                            foundTrades.forEach {
                                logger.info("Found trade: $it")
                                it.status shouldBe Status.PENDING
                                it.setup shouldNotBe null
                                it.setup?.symbol shouldBe "EURUSD"
                                it.setup shouldNotBe null
                                it.setup?.isLong() shouldBe true
                                it.targetPlaceDateTime shouldBe ZonedDateTime.parse("2023-10-30T09:00Z[UTC]")
                            }

                            break
                        }

                        delay(5000L)
                        elapsedTime += 5000L
                    } while (elapsedTime < timeout)

                    if (elapsedTime >= timeout) {
                        fail("No trades found within 30 seconds")
                    }
                }


                val foundTrades = getTrades(accountName, client, mapper)
                val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

                writeMarketDataFile(
                    mapper, marketDataPath, "EURUSD", CurrencyInfo(
                        ask = BigDecimal("3.0"),
                        bid = BigDecimal("3.0"),
                        last = BigDecimal("3.0"),
                        tickValue = BigDecimal("2.0")
                    )
                )




                runBlocking {

                    do {
                        logger.info("Waiting for EA to write file...")
                        val time = getTime(client, mapper)
                        logger.info("Client time $time")
                        delay(5000L)

                    } while (time.isBefore(ZonedDateTime.parse("2023-10-30T09:00Z[UTC]").toLocalDateTime()))


                }


                writeMarketDataFile(
                    mapper, marketDataPath, "EURUSD", CurrencyInfo(
                        ask = BigDecimal("3.0"),
                        bid = BigDecimal("3.0"),
                        last = BigDecimal("3.0"),
                        tickValue = BigDecimal("2.0")
                    )
                )


                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {

                        logger.info("Client time ${getTime(client, mapper)}")
                        val sendTrades = getTrades(accountName, client, mapper)

                        sendTrades.size shouldBe 2

                        val allTradesHaveStatusSent = sendTrades.all {
                            logger.info("Found trade status: ${it.status}")
                            it.status == Status.ORDER_SENT
                        }

                        sendTrades.any { it.id == magicTrade1 } shouldBe true
                        sendTrades.any { it.id == magicTrade2 } shouldBe true

                        writeMarketDataFile(
                            mapper, marketDataPath, "EURUSD", CurrencyInfo(
                                ask = BigDecimal("3.0").add(BigDecimal(0.00001)),
                                bid = BigDecimal("3.0").add(BigDecimal(0.00001)),
                                last = BigDecimal("3.0").add(BigDecimal(0.00001)),
                                tickValue = BigDecimal("2.0").add(BigDecimal(0.00001))
                            )
                        )

                        if (allTradesHaveStatusSent)
                            break


                        delay(5000L)
                        elapsedTime += 5000L
                    } while (elapsedTime < timeout)

                    if (elapsedTime >= timeout) {
                        fail("trades with status orderSent not found within 30 seconds")
                    }
                }


                val openTradeOrder = Orders(
                    accountInfo,
                    mapOf(
                        1 to TradeInfo(
                            magic = magicTrade1!!,
                            lots = BigDecimal("0.01"),
                            symbol = "EURUSD",
                            swap = null,
                            openTime = LocalDateTime.parse("2023-10-30T09:00"),
                            openPrice = BigDecimal("3.0"),
                            stopLoss = BigDecimal("2.0"),
                            takeProfit = BigDecimal("4.0"),
                            type = "buylimit",
                            comment = "test",
                        ),
                        2 to TradeInfo(
                            magic = magicTrade2!!,
                            lots = BigDecimal("0.01"),
                            symbol = "EURUSD",
                            swap = null,
                            openTime = LocalDateTime.parse("2023-10-30T09:00"),
                            openPrice = BigDecimal("3.0"),
                            stopLoss = BigDecimal("2.0"),
                            takeProfit = BigDecimal("4.0"),
                            type = "buylimit",
                            comment = "test",
                        )

                    )
                )

                mapper.writeValue(ordersFile, openTradeOrder)

                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {

                        logger.info("Client time ${getTime(client, mapper)}")
                        val placedInMtTrades = getTrades(accountName, client, mapper)

                        placedInMtTrades.size shouldBe 2

                        val allTradesHaveStatusPlacedInMT = placedInMtTrades.all {
                            logger.info("Found trade status: ${it.status}")
                            it.status == Status.PLACED_IN_MT
                        }

                        writeMarketDataFile(
                            mapper, marketDataPath, "EURUSD", CurrencyInfo(
                                ask = BigDecimal("3.0").add(BigDecimal("0.00001")),
                                bid = BigDecimal("3.0").add(BigDecimal("0.00001")),
                                last = BigDecimal("3.0").add(BigDecimal("0.00001")),
                                tickValue = BigDecimal("2.0").add(BigDecimal("0.00001"))
                            )
                        )

                        if (allTradesHaveStatusPlacedInMT)
                            break


                        delay(5000L)
                        elapsedTime += 5000L
                    } while (elapsedTime < timeout)

                    if (elapsedTime >= timeout) {
                        fail("trades with status placedInMT not found within 30 seconds")
                    }
                }

                writeMarketDataFile(
                    mapper, marketDataPath, "EURUSD", CurrencyInfo(
                        ask = BigDecimal("4.0"),
                        bid = BigDecimal("4.0"),
                        last = BigDecimal("4.0"),
                        tickValue = BigDecimal("4.0")
                    )
                )
            }

//            val result = tradeService.findAll()
//
//            result shouldNotBe null
        }
    }

    private fun checkTradesIsEmpty(
        accountName: String,
        client: OkHttpClient,
        mapper: ObjectMapper
    ) {
        val request = Request.Builder()
            .url("http://localhost:8081/trades/byAccountName/$accountName")
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBodyText = response.body?.string() ?: "Empty Response Body"

            val foundTrades = mapper.readValue<List<TradeDto>>(responseBodyText)
            logger.info("Successfully retrieved trades: $responseBodyText")
            foundTrades.shouldBeEmpty()

        } else {
            logger.info("Failed to retrieve trades: ${response.message}")
            fail("Failed to retrieve trades: ${response.message}")
        }
    }

    private fun getTrades(
        accountName: String,
        client: OkHttpClient,
        mapper: ObjectMapper
    ): List<TradeDto> {
        val request = Request.Builder()
            .url("http://localhost:8081/trades/byAccountName/$accountName")
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBodyText = response.body?.string() ?: "Empty Response Body"

            val foundTrades = mapper.readValue<List<TradeDto>>(responseBodyText)
            logger.info("Successfully retrieved trades: $responseBodyText")

            return foundTrades
        }
        logger.info("Failed to retrieve trades: ${response.message}")
        fail("Failed to retrieve trades: ${response.message}")

    }


    private fun startProcessing(client: OkHttpClient) {
        val startProcessingRequest = Request.Builder()
            .url("http://localhost:8081/actions/start")
            .post("".toRequestBody())
            .build()

        client.newCall(startProcessingRequest).execute()
    }

    private fun stopProcessing(client: OkHttpClient) {
        val startProcessingRequest = Request.Builder()
            .url("http://localhost:8081/actions/stop")
            .post("".toRequestBody())
            .build()

        client.newCall(startProcessingRequest).execute()
    }


    private fun writeMarketDataFile(mapper: ObjectMapper, path: Path, symbol: String, currencyInfo: CurrencyInfo) {
        mapper.writeValue(
            path.toFile(),
            mapOf(
                symbol to currencyInfo
            )
        )
        logger.info("Wrote marketData file: ${path}")
    }

    private fun deleteMarketDataFile(path: Path) {
        if (Files.exists(path)) {
            logger.info("Deleting marketData file: ${path}")
            Files.delete(path)
        }
    }

    private fun setTime(mapper: ObjectMapper, client: OkHttpClient) {
        val json =
            mapper.writeValueAsString(TimeChangeRequest(duration = getDurationBetweenNowAndNextMonday().toMillis()))
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val setTimeRequest = Request.Builder()
            .url("http://localhost:8081/time")
            .put(body)
            .build()

        val setTimeResponse = client.newCall(setTimeRequest).execute()

        getTime(client, mapper)
    }

    private fun getTime(client: OkHttpClient, mapper: ObjectMapper): LocalDateTime {
        val getTimeRequest = Request.Builder()
            .url("http://localhost:8081/time")
            .build()


        val getTimeResponse = client.newCall(getTimeRequest).execute()

        val responseBodyText = getTimeResponse.body?.string() ?: "Empty Response Body"

        val timeDto: TimeDto = mapper.readValue(responseBodyText)

        val instant = Instant.ofEpochMilli(timeDto.time)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

        logger.info("Time set to: ${localDateTime}")
        return localDateTime
    }

    private fun deleteTradesFromTestAccount(client: OkHttpClient, accountName: String) {

        val request = Request.Builder()
            .url("http://localhost:8081/trades/byAccountName/$accountName")
            .delete()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBodyText = response.body?.string() ?: "Empty Response Body"
            val rowsDeleted = responseBodyText.toIntOrNull() ?: "Failed to parse response body to Int"
            logger.info("Successfully deleted $rowsDeleted trades for account: $accountName")
        } else {
            logger.info("Failed to delete trades: ${response.message}")
            fail("Failed to delete trades: ${response.message}")
        }
    }

    private fun deleteSetupsFromTestAccount(client: OkHttpClient, accountName: String) {

        val request = Request.Builder()
            .url("http://localhost:8081/setups/byAccountName/$accountName")
            .delete()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBodyText = response.body?.string() ?: "Empty Response Body"
            val rowsDeleted = responseBodyText.toIntOrNull() ?: "Failed to parse response body to Int"
            logger.info("Successfully deleted $rowsDeleted setups for account: $accountName")
        } else {
            logger.info("Failed to delete setups: ${response.message}")
            fail("Failed to delete setups: ${response.message}")
        }
    }

    private fun getDurationBetweenNowAndNextMonday(): Duration {

        val today = LocalDate.now(ZoneOffset.UTC)
        val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val nextMondayAt859 = ZonedDateTime.of(nextMonday.atTime(8, 59, 50), ZoneOffset.UTC)
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        return Duration.between(now, nextMondayAt859)
        //return Clock.offset(Clock.system(ZoneOffset.UTC), durationUntilNextMondayAt859)

    }

    private fun deleteFilesBeforeTest(path: Path, prefix: String, suffix: String) {
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

}