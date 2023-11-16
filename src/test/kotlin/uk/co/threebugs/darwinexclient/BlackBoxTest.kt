import io.kotest.core.spec.style.*
import io.kotest.core.test.*
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import kotlinx.coroutines.*
import uk.co.threebugs.darwinexclient.*
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.deleteFilesBeforeTest
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.deleteMarketDataFile
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.readOrdersFile
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeEmptyOrders
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeMarketData
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeOrdersFile
import uk.co.threebugs.darwinexclient.helpers.MetaTraderFileHelper.Companion.writeOrdersWithMagic
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.deleteTradesFromSetupGroupsName
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.getTradesWithSetupGroupsName
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.startProcessing
import uk.co.threebugs.darwinexclient.helpers.RestCallHelper.Companion.stopProcessing
import uk.co.threebugs.darwinexclient.helpers.TimeHelper.Companion.getTime
import uk.co.threebugs.darwinexclient.helpers.TimeHelper.Companion.setClockToSpecificDateTime
import uk.co.threebugs.darwinexclient.helpers.TimeOutHelper.Companion.waitForCondition
import uk.co.threebugs.darwinexclient.utils.*
import java.lang.System.*
import java.nio.file.*
import java.time.*


private const val SECONDS_30 = 30000L
private const val SECONDS_5 = 5000L
private const val EURUSD = "EURUSD"

class BlackBoxTest : AnnotationSpec() {

    data class TestSetup(val setupGroupsName: String, val isLong: Boolean)

    private val testSetupMap = mapOf(
        "longSetup" to TestSetup("long-test", true),
        "shortSetup" to TestSetup("short-test", false)
    )

    private suspend fun beforeEach(setupGroupsName: String) {
        deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")

        writeEmptyOrders()
        deleteTradesFromSetupGroupsName(setupGroupsName)
        deleteMarketDataFile()
        getTradesWithSetupGroupsName(setupGroupsName).shouldBeEmpty()
        setClockToSpecificDateTime(ZonedDateTime.parse("2023-10-30T08:59:40.000Z"))
        startProcessing()
        delay(5000)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {

        writeEmptyOrders()

        delay(SECONDS_5)

        stopProcessing()

        //deleteFilesBeforeTest(Path.of("test-ea-files/DWX"), "DWX_Commands_", ".txt")

        super.afterEach(testCase, result)
    }


    val testSetup =
        testSetupMap[getProperty("testSetup")]
            ?: throw IllegalArgumentException("testSetup ${getProperty("testSetup")} not found in testSetupMap")


    val buySell = if (testSetup.isLong) "buy" else "sell"

    @Test
    suspend fun place2EurusdLongTradesAndCloseAtTime() {
        beforeEach(testSetup.setupGroupsName)
        writeMarketData(EURUSD)

        val nextMondayAt9 = ZonedDateTime.parse("2023-10-30T09:00:00.000Z")
        val nextNextMondayAt9 = ZonedDateTime.parse("2023-11-06T09:00Z")
        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status PENDING to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val foundTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            if (foundTrades.isNotEmpty()) {

                foundTrades.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.PENDING
                    it.createdDateTime shouldNotBe null
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime().toString() shouldBe nextMondayAt9
                        .toString()
                }

                return@waitForCondition true
            }
            false
        }

        val foundTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

        //We should have 2 trades with status PENDING
        foundTrades.size shouldBe 2

        val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

        writeMarketData(EURUSD)


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for the time to be 09:00..."
        ) {

            logger.info("Waiting for EA to write file...")
            val time = getTime()
            logger.info("Client time $time")

            if (!time.isBefore(nextMondayAt9))
                return@waitForCondition true

            false
        }

        writeMarketData(EURUSD)


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be OrderSent..."
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithStatusOrderSent = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            val allTradesHaveStatusSent = tradesWithStatusOrderSent.count {
                it.status == Status.ORDER_SENT
            }

            tradesWithStatusOrderSent.any { it.id == magicTrade1 } shouldBe true
            tradesWithStatusOrderSent.any { it.id == magicTrade2 } shouldBe true

            writeMarketData(EURUSD)

            if (allTradesHaveStatusSent == 2) {
                return@waitForCondition true
            }

            false
        }

        writeOrdersWithMagic(magicTrade1, magicTrade2, "EURUSD", "${buySell}limit")

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be placed in MT..."
        ) {

            logger.info("Client time ${getTime()}")
            val trades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            //Once we send the trades to MT we should have 4 trades. 2 with status PENDING and 2 with status PLACED_IN_MT
            trades.size shouldBe 4

            val allTradesHaveStatusPlacedInMT = trades.count {
                it.status == Status.PLACED_IN_MT
            }

            writeMarketData(EURUSD)

            val nextPendingTrades = trades.count {
                it.status == Status.PENDING
            }

            nextPendingTrades shouldBe 2

            if (allTradesHaveStatusPlacedInMT == 2)
                return@waitForCondition true

            false
        }

        delay(5000)

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_0.txt")) shouldBe true
        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_1.txt")) shouldBe true
        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_2.txt")) shouldBe false

        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_0.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true
        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_1.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true

        writeMarketData(EURUSD)

        val ordersAndAccount = readOrdersFile()
        ordersAndAccount.orders.size shouldBe 2

        ordersAndAccount.orders[1]?.type = buySell
        ordersAndAccount.orders[2]?.type = buySell

        writeOrdersFile(ordersAndAccount)

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for the 2 trades we placed early to have status filled..."
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithSetupGroupName = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            // 2 trades with status PENDING and 2 trades with status FILLED
            tradesWithSetupGroupName.size shouldBe 4

            val allTradesHaveStatusFilled = tradesWithSetupGroupName.count {
                it.status == Status.FILLED
            }

            writeMarketData(EURUSD)

            if (allTradesHaveStatusFilled == 2)
                return@waitForCondition true

            false
        }

        writeMarketData(EURUSD)

        val filledTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

        val filledTrade1 = filledTrades.first { t -> t.id == magicTrade1 }

        filledTrades.size shouldBe 4

        val targetPlaceDateTime = filledTrade1.targetPlaceDateTime
        val tradeDurationInHours = filledTrade1.setup.tradeDuration.toLong()
        val timeJustBeforeClose = targetPlaceDateTime!!.plusHours(tradeDurationInHours).minusSeconds(10)
        logger.info("targetPlaceDateTime: $targetPlaceDateTime tradeDurationInHours: $tradeDurationInHours timeJustBeforeClose: $timeJustBeforeClose")

        setClockToSpecificDateTime(timeJustBeforeClose)
        //  setTimeToNearlyCloseTime(filledTrades[1])


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to have status closed by magic sent"
        ) {

            logger.info("Client time ${getTime()}")
            val closedByUserTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)


            val closedByMagicSentCount = closedByUserTrades.count { it.status == Status.CLOSED_BY_MAGIC_SENT }
            logger.info("Number of trades with status CLOSED_BY_MAGIC_SENT: $closedByMagicSentCount")

            if (closedByMagicSentCount == 2) {
                return@waitForCondition true
            }
            writeMarketData(EURUSD)

            false
        }

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status PENDING to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val foundPendingTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            val pendingCount = foundPendingTrades.count { it.status == Status.PENDING }

            if (pendingCount == 2) {
                logger.info("backend time: ${getTime()}")
                foundPendingTrades.filter { it.status == Status.PENDING }.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.PENDING
                    it.createdDateTime shouldNotBe null
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime().toString() shouldBe nextNextMondayAt9.toString()
                }

                return@waitForCondition true
            }
            false
        }


        delay(5000)

        val targetFileCount = 4
        val directory = Path.of("test-ea-files/DWX")
        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for 4 DWX_Commands_*.txt files to be written to the DWX directory"
        ) {

            writeMarketData(EURUSD)
            val files = Files.list(directory)
                .filter { path -> path.fileName.toString().startsWith("DWX_Commands_") }
                .toList()


            if (files.size == targetFileCount)
                return@waitForCondition true

            false
        }

        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_0.txt"))
            .contains("|OPEN_ORDER|EURUSD,${buySell}limit") shouldBe true
        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_1.txt"))
            .contains("|OPEN_ORDER|EURUSD,${buySell}limit") shouldBe true

        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_2.txt"))
            .contains("|CLOSE_ORDERS_BY_MAGIC|") shouldBe true
        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_3.txt"))
            .contains("|CLOSE_ORDERS_BY_MAGIC|") shouldBe true

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_4.txt")) shouldBe false

        writeMarketData(EURUSD)

        writeEmptyOrders()

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to have status closed by time"
        ) {

            logger.info("Client time ${getTime()}")
            val closedByUserTrades =
                getTradesWithSetupGroupsName(testSetup.setupGroupsName).count { it.status == Status.CLOSED_BY_TIME }

            writeMarketData(EURUSD)

            if (closedByUserTrades == 2)
                return@waitForCondition true

            false
        }
    }


    @Test
    suspend fun place2EurusdTradesAndCloseByUser() {
        beforeEach(testSetup.setupGroupsName)

        writeMarketData(EURUSD)

        val nextMondayAt9 = ZonedDateTime.parse("2023-10-30T09:00:00.000Z")

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status PENDING to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val foundTrades =
                getTradesWithSetupGroupsName(testSetup.setupGroupsName).filter { it.status == Status.PENDING }

            if (foundTrades.isNotEmpty()) {

                foundTrades.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.PENDING
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime() shouldBe nextMondayAt9.toOffsetDateTime()
                }

                return@waitForCondition true
            }
            false
        }


        val foundTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)
        val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

        writeMarketData(EURUSD)

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for the time to be 09:00..."
        ) {

            logger.info("Waiting for EA to write file...")
            val time = getTime()
            logger.info("Client time $time")

            if (!time.isBefore(nextMondayAt9))
                return@waitForCondition true

            false
        }

        writeMarketData(EURUSD)


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be OrderSent..."
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithStatusOrderSent = getTradesWithSetupGroupsName(testSetup.setupGroupsName)


            val tradesWithSentCount = tradesWithStatusOrderSent.count { it.status == Status.ORDER_SENT }

            tradesWithStatusOrderSent.any { it.id == magicTrade1 } shouldBe true
            tradesWithStatusOrderSent.any { it.id == magicTrade2 } shouldBe true

            writeMarketData(EURUSD)

            if (tradesWithSentCount == 2) {
                return@waitForCondition true
            }

            false
        }

        writeOrdersWithMagic(magicTrade1, magicTrade2, "EURUSD", "${buySell}limit")

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be placed in MT..."
        ) {

            logger.info("Client time ${getTime()}")
            val trades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            trades.size shouldBe 4

            val tradesWithStatusPlacedInMTCount = trades.count {
                it.status == Status.PLACED_IN_MT
            }

            trades.count { it.status == Status.PENDING } shouldBe 2

            writeMarketData(EURUSD)

            if (tradesWithStatusPlacedInMTCount == 2)
                return@waitForCondition true

            false
        }

        delay(5000)

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_0.txt")) shouldBe true
        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_1.txt")) shouldBe true

        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_0.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true
        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_1.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true

        writeMarketData(EURUSD)

        val ordersAndAccount = readOrdersFile()
        ordersAndAccount.orders.size shouldBe 2

        ordersAndAccount.orders[1]?.type = buySell
        ordersAndAccount.orders[2]?.type = buySell

        writeOrdersFile(ordersAndAccount)

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to have status filled..."
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithSetupGroupsName = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            tradesWithSetupGroupsName.size shouldBe 4

            val tradesWithStatusFilledCount = tradesWithSetupGroupsName.count { it.status == Status.FILLED }

            writeMarketData(EURUSD)

            tradesWithSetupGroupsName.count { it.status == Status.PENDING } shouldBe 2

            if (tradesWithStatusFilledCount == 2)
                return@waitForCondition true

            false
        }

        writeMarketData(EURUSD)

        writeEmptyOrders()

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to have status closed by user"
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithSetupGroupName = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            tradesWithSetupGroupName.size shouldBe 4

            val closedByUserCount = tradesWithSetupGroupName.count {
                it.status == Status.CLOSED_BY_USER
            }

            writeMarketData(EURUSD)

            if (closedByUserCount == 2)
                return@waitForCondition true

            false
        }

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_2.txt")) shouldBe false
    }

    @Test
    suspend fun place2EurusdTradesAndOutOfTimeClose() {

        beforeEach(testSetup.setupGroupsName)
        writeMarketData(EURUSD)

        val nextMondayAt9 = ZonedDateTime.parse("2023-10-30T09:00:00.000Z")
        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status PENDING to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val foundTrades =
                getTradesWithSetupGroupsName(testSetup.setupGroupsName).filter { it.status == Status.PENDING }

            if (foundTrades.isNotEmpty()) {

                foundTrades.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.PENDING
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup shouldNotBe null
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime().toString() shouldBe nextMondayAt9
                        .toString()

                }

                return@waitForCondition true
            }
            false
        }


        val foundTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)
        val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

        writeMarketData(EURUSD)


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for the time to be 09:00..."
        ) {

            logger.info("Waiting for EA to write file...")
            val time = getTime()
            logger.info("Client time $time")

            if (!time.isBefore(nextMondayAt9))
                return@waitForCondition true

            false
        }

        writeMarketData(EURUSD)


        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be OrderSent..."
        ) {

            logger.info("Client time ${getTime()}")
            val tradesWithStatusOrderSent = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            val tradesWithOrderSentCount = tradesWithStatusOrderSent.count {
                it.status == Status.ORDER_SENT
            }

            tradesWithStatusOrderSent.any { it.id == magicTrade1 } shouldBe true
            tradesWithStatusOrderSent.any { it.id == magicTrade2 } shouldBe true

            writeMarketData(EURUSD)

            if (tradesWithOrderSentCount == 2) {
                return@waitForCondition true
            }

            false
        }

        writeOrdersWithMagic(magicTrade1, magicTrade2, "EURUSD", "${buySell}limit")

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to be placed in MT..."
        ) {

            logger.info("Client time ${getTime()}")
            val trades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            trades.size shouldBe 4

            val tradesHaveCorrectStatus = trades.count {
                it.status == Status.PLACED_IN_MT
            }

            writeMarketData(EURUSD)

            if (tradesHaveCorrectStatus == 2)
                return@waitForCondition true

            false
        }

        delay(5000)

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_0.txt")) shouldBe true
        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_1.txt")) shouldBe true

        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_0.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true
        Files.readString(Path.of("test-ea-files/DWX/DWX_Commands_1.txt"))
            .contains("OPEN_ORDER|EURUSD,${buySell}limit,") shouldBe true

        writeMarketData(EURUSD)


        writeEmptyOrders()

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for all trades to have status out of time"
        ) {

            logger.info("Client time ${getTime()}")
            val trades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)

            val tradesHaveCorrectStatus = trades.count {
                it.status == Status.OUT_OF_TIME
            }

            writeMarketData(EURUSD)

            if (tradesHaveCorrectStatus == 2)
                return@waitForCondition true

            false
        }

        Files.exists(Path.of("test-ea-files/DWX/DWX_Commands_2.txt")) shouldBe false
    }

    @Test
    suspend fun place2MissedEurusdTrades() {
        beforeEach(testSetup.setupGroupsName)
        writeMarketData(EURUSD)

        val nextMondayAt9 = ZonedDateTime.parse("2023-10-30T09:00:00.000Z")
        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status PENDING to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val foundTrades =
                getTradesWithSetupGroupsName(testSetup.setupGroupsName).filter { it.status == Status.PENDING }

            if (foundTrades.isNotEmpty()) {

                foundTrades.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.PENDING
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup shouldNotBe null
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime().toString() shouldBe nextMondayAt9
                        .toString()

                }

                return@waitForCondition true
            }
            false
        }


        val foundTrades = getTradesWithSetupGroupsName(testSetup.setupGroupsName)
        val (magicTrade1, magicTrade2) = foundTrades.take(2).map { it.id }

        setClockToSpecificDateTime(ZonedDateTime.parse("2023-10-31T08:59:40.000Z"))

        writeMarketData(EURUSD)

        waitForCondition(
            timeout = SECONDS_30,
            interval = SECONDS_5,
            logMessage = "Waiting for trades with status MISSED to be written to the db..."
        ) {
            logger.info("Client time ${getTime()}")
            val missedTrades =
                getTradesWithSetupGroupsName(testSetup.setupGroupsName).filter { it.status == Status.MISSED }

            if (missedTrades.isNotEmpty()) {

                missedTrades.forEach {
                    logger.info("Found trade: $it")
                    it.status shouldBe Status.MISSED
                    it.setup shouldNotBe null
                    it.setup.symbol shouldBe EURUSD
                    it.setup shouldNotBe null
                    it.setup.isLong() shouldBe testSetup.isLong
                    it.targetPlaceDateTime!!.toOffsetDateTime().toString() shouldBe nextMondayAt9
                        .toString()

                }

                return@waitForCondition true
            }
            false
        }

    }
}
