import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.deleteFilesBeforeTest
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.deleteMarketDataFile
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeEmptyOrders
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeMarketData
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeOrdersWithMagic
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.deleteTradesFromTestAccount
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.getTrades
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.startProcessing
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.stopProcessing
import uk.co.threebugs.darwinexclient.helpers.TimeHelper.Companion.getTime
import uk.co.threebugs.darwinexclient.helpers.TimeHelper.Companion.setTimeToNextMonday
import uk.co.threebugs.darwinexclient.utils.logger
import java.nio.file.Path
import java.time.ZonedDateTime


class TradeServiceTest : FunSpec() {

    private val accountName = "test"

    override suspend fun beforeEach(testCase: TestCase) {
        deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")
        //    deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Orders", ".json")

        writeEmptyOrders()

        //mapper.writeValue(ordersStoredFile, emptyOrders)

        deleteTradesFromTestAccount(accountName)
        //deleteSetupsFromTestAccount(client, accountName)

        deleteMarketDataFile()

        getTrades(accountName).shouldBeEmpty()

        setTimeToNextMonday()

        startProcessing()

        delay(5000)


        super.beforeEach(testCase)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {

        writeEmptyOrders()
        // mapper.writeValue(ordersStoredFile, emptyOrders)

        delay(5000L)

        stopProcessing()

        deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")

        super.afterEach(testCase, result)
    }

    init {

        test("place 2 eurusd long trades") {
            runBlocking {

                writeMarketData("EURUSD")

                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {
                        logger.info("Waiting for EA to write file...")
                        logger.info("Client time ${getTime()}")
                        val foundTrades = getTrades(accountName)

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


                val foundTrades = getTrades(accountName)
                val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

                writeMarketData("EURUSD")

                runBlocking {

                    do {
                        logger.info("Waiting for EA to write file...")
                        val time = getTime()
                        logger.info("Client time $time")
                        delay(5000L)

                    } while (time.isBefore(ZonedDateTime.parse("2023-10-30T09:00Z[UTC]").toLocalDateTime()))


                }


                writeMarketData("EURUSD")


                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {

                        logger.info("Client time ${getTime()}")
                        val sendTrades = getTrades(accountName)

                        sendTrades.size shouldBe 2

                        val allTradesHaveStatusSent = sendTrades.all {
                            logger.info("Found trade status: ${it.status}")
                            it.status == Status.ORDER_SENT
                        }

                        sendTrades.any { it.id == magicTrade1 } shouldBe true
                        sendTrades.any { it.id == magicTrade2 } shouldBe true

                        writeMarketData("EURUSD")

                        if (allTradesHaveStatusSent)
                            break


                        delay(5000L)
                        elapsedTime += 5000L
                    } while (elapsedTime < timeout)

                    if (elapsedTime >= timeout) {
                        fail("trades with status orderSent not found within 30 seconds")
                    }
                }

                writeOrdersWithMagic(magicTrade1, magicTrade2)

                runBlocking {
                    var elapsedTime = 0L
                    val timeout = 30000L  // 30 seconds in milliseconds

                    do {

                        logger.info("Client time ${getTime()}")
                        val placedInMtTrades = getTrades(accountName)

                        placedInMtTrades.size shouldBe 2

                        val allTradesHaveStatusPlacedInMT = placedInMtTrades.all {
                            logger.info("Found trade status: ${it.status}")
                            it.status == Status.PLACED_IN_MT
                        }

                        writeMarketData("EURUSD")

                        if (allTradesHaveStatusPlacedInMT)
                            break


                        delay(5000L)
                        elapsedTime += 5000L
                    } while (elapsedTime < timeout)

                    if (elapsedTime >= timeout) {
                        fail("trades with status placedInMT not found within 30 seconds")
                    }
                }

                writeMarketData("EURUSD")
            }
        }
    }
}