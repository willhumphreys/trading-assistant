package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.SlackClient
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroups
import uk.co.threebugs.darwinexclient.clock.MutableClock
import uk.co.threebugs.darwinexclient.metatrader.Client
import uk.co.threebugs.darwinexclient.metatrader.Order
import uk.co.threebugs.darwinexclient.metatrader.TradeInfo
import uk.co.threebugs.darwinexclient.setup.Setup
import uk.co.threebugs.darwinexclient.setup.SetupFileRepository
import uk.co.threebugs.darwinexclient.setup.SetupRepository
import uk.co.threebugs.darwinexclient.utils.Constants
import uk.co.threebugs.darwinexclient.utils.TimeHelper
import uk.co.threebugs.darwinexclient.utils.logger
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val tradeMapper: TradeMapper,
    private val timeHelper: TimeHelper,
    private val slackClient: SlackClient,
    private val clock: MutableClock,
    private val setupRepository: SetupRepository,
) {
    fun findById(id: Int): TradeDto? {
        return tradeRepository.findByIdOrNull(id)?.let { tradeMapper.toDto(it) }
    }


    fun save(tradeDto: TradeDto): TradeDto {
        val setup = findSetupById(tradeDto.setup!!.id!!)

        return tradeMapper.toEntity(tradeDto, setup, clock)
            .let(tradeRepository::save)
            .let(tradeMapper::toDto)
    }

    private fun findSetupById(id: Int): Setup {
        return setupRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Setup not found for ID: $id")
    }

    fun deleteById(id: Int) {
        tradeRepository.deleteById(id)
    }

    fun deleteTradesBySetupGroupsName(name: String): Int {
        return tradeRepository.deleteBySetupGroupsName(name)
    }

    fun findAll(): List<TradeDto> {
        return tradeRepository.findAll().filterNotNull().map { tradeMapper.toDto(it) }
    }

    fun findTrades(exampleRecord: TradeDto, sort: Sort): List<TradeDto?> {
        val example = Example.of(tradeMapper.toEntity(exampleRecord, clock))

        return tradeRepository.findAll(example, sort).map { tradeMapper.toDto(it) }
    }

    fun createTradesToPlaceFromEnabledSetups(symbol: String, accountSetupGroups: AccountSetupGroups) {

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")


        val setups = setupRepository.findEnabledSetups(symbol, accountSetupGroups.setupGroups!!)
        setups.forEach { setup ->
            val targetPlaceTime: ZonedDateTime =
                SetupFileRepository.getNextEventTime(setup.dayOfWeek!!, setup.hourOfDay!!, clock)
            //logger.info("clockNow: ${ZonedDateTime.now()} targetPlaceTime: ${formatter.format(targetPlaceTime)}" + " setup: ${setup.id}" + " accountSetupGroups: ${accountSetupGroups.id}")
            val existingTrade =
                tradeRepository.findBySetupAndTargetPlaceDateTimeAndAccountSetupGroups(
                    accountSetupGroups.id!!,
                    setup.id!!,
                    formatter.format(targetPlaceTime)
                )
            if (existingTrade == null) {
                val trade = tradeMapper.toEntity(setup, targetPlaceTime, accountSetupGroups.account!!, clock).apply {
                    status = Status.PENDING
                }
                tradeRepository.save(trade)
                slackClient.sendSlackNotification(trade.newTradeMessage)
            }
        }
    }


    fun placeTrades(
        dwx: Client,
        symbol: String,
        bid: BigDecimal,
        ask: BigDecimal,
        accountSetupGroups: AccountSetupGroups
    ) {
        tradeRepository.findByAccountSetupGroupsSymbolAndStatus(accountSetupGroups.id!!, symbol, Status.PENDING.name)
            .forEach(Consumer { trade: Trade ->
                val now = ZonedDateTime.now(clock)
                val targetPlaceDateTime = trade.targetPlaceDateTime
                if (now.isAfter(targetPlaceDateTime)) {
                    tradeRepository.save(placeTrade(dwx, bid, ask, trade))
                }
            })
    }

    fun placeTrade(dwx: Client, bid: BigDecimal, ask: BigDecimal, trade: Trade): Trade {
        val fillPrice = if (trade.setup!!.isLong) ask else bid
        val orderType = if (trade.setup!!.isLong) "buylimit" else "selllimit"
        var tickSize = BigDecimal("0.00001")
        if (trade.setup!!.symbol.equals(Constants.USDJPY, ignoreCase = true)) {
            tickSize = BigDecimal("0.01")
        }
        val price = addTicks(fillPrice, trade.setup!!.tickOffset!!, tickSize)
        val stopLoss = addTicks(fillPrice, trade.setup!!.stop!!, tickSize)
        val takeProfit = addTicks(fillPrice, trade.setup!!.limit!!, tickSize)


        //        dwx.openOrder(Order.builder()
//                           .symbol("EURUSD")
//                           .orderType("buylimit")
//                           .lots(0.01)
//                           .price(new BigDecimal("1.02831"))
//                           .stopLoss(new BigDecimal("1.00831"))
//                           .takeProfit(new BigDecimal("1.0931"))
//                           .magic(100)
//                           .comment("test")
//                           .expiration(TradeService.addSecondsToCurrentTime(8))
//                           .build());
        val magic = trade.id
        dwx.openOrder(
            Order(
                symbol = trade.setup!!.symbol,
                orderType = orderType,
                lots = 0.01,
                price = price,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                magic = magic,
                expiration = timeHelper.addSecondsToCurrentTime(trade.setup!!.outOfTime!!.toLong()),
                comment = trade.setup!!.concatenateFields()
            )
        )

        trade.status = Status.ORDER_SENT
        slackClient.sendSlackNotification("Order placed: " + trade.setup!!.concatenateFields())
        return trade
    }

    fun closeTradesAtTime(dwx: Client, symbol: String, accountSetupGroups: AccountSetupGroups) {
        tradeRepository.findByAccountSetupGroupsSymbolAndStatus(accountSetupGroups.id!!, symbol, Status.FILLED.name)
            .stream()
            .filter { trade: Trade ->

                val closeDateTime = trade.targetPlaceDateTime!!.plusHours(trade.setup!!.tradeDuration!!.toLong())
                val currentDateTime = ZonedDateTime.now(clock)

                //logger.info("closeDateTime: $closeDateTime currentDateTime: $currentDateTime")

                closeDateTime.isBefore(currentDateTime)
            }.forEach { trade: Trade ->
                dwx.closeOrdersByMagic(trade.id!!)
                trade.status = Status.CLOSED_BY_MAGIC_SENT
                //trade.closedDateTime = ZonedDateTime.now()
                tradeRepository.save(trade)
                slackClient.sendSlackNotification("Order closed by magic: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
            }
    }

    fun fillTrade(tradeInfo: TradeInfo, metatraderId: Int) {
        tradeRepository.findByIdOrNull(tradeInfo.magic)?.let { trade ->
            if (trade.status == Status.PENDING || trade.status == Status.ORDER_SENT) {
                trade.apply {
                    placedPrice = tradeInfo.openPrice
                    placedDateTime = ZonedDateTime.of(tradeInfo.openTime, ZoneId.of("Europe/Zurich"))
                    status = Status.PLACED_IN_MT
                    this.metatraderId = metatraderId
                }.also {
                    tradeRepository.save(it)
                    slackClient.sendSlackNotification("Order placed in MT: $it")
                }
            }
        } ?: logger.warn("Trade not found: $tradeInfo")
    }

    fun closeTrade(tradeInfo: TradeInfo) {
        tradeRepository.findByIdOrNull(tradeInfo.magic)?.let { trade ->
            closeTrade(
                tradeInfo,
                trade
            )
        } ?: logger.warn("Trade not found: $tradeInfo")
    }

    private fun closeTrade(tradeInfo: TradeInfo, trade: Trade) {

        val closingStatus =

            if (trade.status == Status.CLOSED_BY_MAGIC_SENT) {
                Status.CLOSED_BY_TIME
            } else if (tradeInfo.type.equals("buy") || tradeInfo.type.equals("sell")) {
                Status.CLOSED_BY_USER
            } else if (tradeInfo.type.equals("buylimit") || tradeInfo.type.equals("selllimit")) {
                Status.OUT_OF_TIME
            } else {
                Status.CLOSED_BY_USER
            }

        trade.apply {
            status = closingStatus
            closedPrice = tradeInfo.takeProfit
            closedDateTime = ZonedDateTime.now(clock)
            profit = tradeInfo.profitAndLoss
        }.also { tradeRepository.save(it) }

        slackClient.sendSlackNotification("Order closed: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
    }

    fun deleteTradesByAccountName(name: String): Int {
        return tradeRepository.deleteByAccountName(name)
    }

    fun findByAccountName(name: String): List<TradeDto> {
        return tradeRepository.findByAccount_Name(name).map { tradeMapper.toDto(it) }
    }

    fun findBySetupGroupsName(name: String): List<TradeDto>? {
        return tradeRepository.findBySetupGroupsName(name).map { tradeMapper.toDto(it) }
    }

    companion object {
        fun addTicks(initialPrice: BigDecimal, ticksToAdd: Int, tickSize: BigDecimal): BigDecimal {
            val ticks = tickSize.multiply(BigDecimal.valueOf(ticksToAdd.toLong()))
            return initialPrice.add(ticks)
        }
    }
}
