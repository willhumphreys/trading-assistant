package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.co.threebugs.darwinexclient.SlackClient
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroupsDto
import uk.co.threebugs.darwinexclient.clock.MutableClock
import uk.co.threebugs.darwinexclient.metatrader.Order
import uk.co.threebugs.darwinexclient.metatrader.TradeInfo
import uk.co.threebugs.darwinexclient.metatrader.commands.CommandService
import uk.co.threebugs.darwinexclient.metatrader.data.MANUAL_SETUP_NAME
import uk.co.threebugs.darwinexclient.modifiers.ModifierRepository
import uk.co.threebugs.darwinexclient.search.TradeSearchDto
import uk.co.threebugs.darwinexclient.setup.Setup
import uk.co.threebugs.darwinexclient.setup.SetupFileRepository
import uk.co.threebugs.darwinexclient.setup.SetupRepository
import uk.co.threebugs.darwinexclient.setupgroup.Direction
import uk.co.threebugs.darwinexclient.setupmodifier.SetupModifierRepository
import uk.co.threebugs.darwinexclient.tradingstance.TradingStanceRepository
import uk.co.threebugs.darwinexclient.tradingstance.UpdateTradingStanceDto
import uk.co.threebugs.darwinexclient.utils.Constants
import uk.co.threebugs.darwinexclient.utils.TimeHelper
import uk.co.threebugs.darwinexclient.utils.logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

@Transactional
@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val tradeMapper: TradeMapper,
    private val tradeSearchMapper: TradeSearchMapper,
    private val timeHelper: TimeHelper,
    private val slackClient: SlackClient,
    private val clock: MutableClock,
    private val setupRepository: SetupRepository,
    private val setupModifierRepository: SetupModifierRepository,
    private val commandService: CommandService,
    private val tradingStanceRepository: TradingStanceRepository,
    private val modifierRepository: ModifierRepository
) {


    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    fun findById(id: Int): TradeDto? {
        return tradeRepository.findByIdOrNull(id)?.let { tradeMapper.toDto(it) }
    }

    fun save(tradeDto: TradeDto): TradeDto {
        val setup = findSetupById(tradeDto.setup.id!!)

        return tradeMapper.toEntity(tradeDto, setup, clock).let(tradeRepository::save).let(tradeMapper::toDto)
    }

    private fun findSetupById(id: Int): Setup {
        return setupRepository.findByIdOrNull(id) ?: throw IllegalArgumentException("Setup not found for ID: $id")
    }

    fun deleteById(id: Int) {
        tradeRepository.deleteById(id)
    }

    fun deleteTradesBySetupGroupsName(name: String): Int {
        return tradeRepository.deleteBySetupGroupsName(name)
    }

    fun findAll(sort: Sort): List<TradeDto> {
        return tradeRepository.findAll(sort).filterNotNull().map { tradeMapper.toDto(it) }
    }

    fun findTrades(exampleRecord: TradeSearchDto, sort: Sort): List<TradeSearchDto> {
        val example = Example.of(tradeSearchMapper.toEntity(exampleRecord))

        return tradeRepository.findAll(example, sort).map { tradeSearchMapper.toDto(it) }
    }

    fun createTradesToPlaceFromEnabledSetups(symbol: String, accountSetupGroups: AccountSetupGroupsDto) {

        val setups = setupRepository.findEnabledSetups(symbol, accountSetupGroups.setupGroups.name)

        val tradingStance =
            tradingStanceRepository.findBySymbolAndAccountSetupGroups_Name(symbol, accountSetupGroups.name)
                ?: throw IllegalArgumentException("Trading stance not found for symbol: $symbol and accountSetupGroups: ${accountSetupGroups.name}")

        setups.filter { s -> (s.direction == tradingStance.direction || tradingStance.direction == Direction.BOTH) && s.name != MANUAL_SETUP_NAME }

            .forEach { setup ->
                val targetPlaceTime: ZonedDateTime =
                    SetupFileRepository.getNextEventTime(setup.dayOfWeek!!, setup.hourOfDay!!, clock)
                //logger.info("clockNow: ${ZonedDateTime.now()} targetPlaceTime: ${formatter.format(targetPlaceTime)}" + " setup: ${setup.id}" + " accountSetupGroups: ${accountSetupGroups.id}")

                val existingTrades = tradeRepository.findBySetupAndTargetPlaceDateTimeAndAccountSetupGroups(
                    accountSetupGroups.id!!, setup.id!!, formatter.format(targetPlaceTime)
                )
                if (existingTrades.isEmpty() || existingTrades.all { it.status == Status.CANCELLED_BY_STANCE }) {
                    val now = ZonedDateTime.now(clock)

                    val trade = tradeMapper.toEntity(setup, targetPlaceTime, accountSetupGroups.account, clock)
                    if (now.isAfter(targetPlaceTime.plusHours(setup.outOfTime!!.toLong()))) {
                        logger.info(
                            "Skipping trade as the trade windows has been closed: ${
                                formatter.format(
                                    targetPlaceTime
                                )
                            }"
                        )

                        tradeRepository.save(trade.apply {
                            status = Status.MISSED
                        })
                    } else {
                        tradeRepository.save(trade.apply {
                            status = Status.PENDING
                        })
                    }
                }
            }
    }


    fun placeTrades(
        symbol: String, bid: BigDecimal, ask: BigDecimal, accountSetupGroups: AccountSetupGroupsDto
    ) {
        tradeRepository.findByAccountSetupGroupsSymbolAndStatus(accountSetupGroups.id!!, symbol, Status.PENDING.name)
            .forEach(Consumer { trade: Trade ->
                val now = ZonedDateTime.now(clock)
                val targetPlaceDateTime = trade.targetPlaceDateTime
                if (now.isAfter(targetPlaceDateTime!!.plusHours(trade.setup!!.outOfTime!!.toLong()))) {
                    tradeRepository.save(trade.apply {
                        status = Status.MISSED
                        lastUpdatedDateTime = ZonedDateTime.now(clock)
                    })
                } else if (now.isAfter(targetPlaceDateTime)) {
                    tradeRepository.save(placeTrade(bid, ask, trade, accountSetupGroups))
                }
            })
    }

    fun placeTrade(
        bid: BigDecimal, ask: BigDecimal, trade: Trade, accountSetupGroupsDto: AccountSetupGroupsDto
    ): Trade {
        val fillPrice = if (trade.setup!!.isLong) ask else bid
        val orderType = when {
            trade.setup!!.isLong && trade.setup!!.tickOffset!! > 0 -> "buystop"
            trade.setup!!.isLong && trade.setup!!.tickOffset!! == 0 -> "buy"
            trade.setup!!.isLong && trade.setup!!.tickOffset!! < 0 -> "buylimit"
            !trade.setup!!.isLong && trade.setup!!.tickOffset!! < 0 -> "sellstop"
            !trade.setup!!.isLong && trade.setup!!.tickOffset!! == 0 -> "sell"
            !trade.setup!!.isLong && trade.setup!!.tickOffset!! > 0 -> "selllimit"
            else -> throw IllegalArgumentException("Cannot determine order type for trade ID: ${trade.id}")
        }

        var lotSize = BigDecimal("0.01")
        var tickSize = BigDecimal("0.00001")

        // Handle special symbol conditions
        if (trade.setup!!.symbol.equals(Constants.USDJPY, ignoreCase = true)) {
            tickSize = BigDecimal("0.001")
        } else if (trade.setup!!.symbol.equals(Constants.XAUUSD, ignoreCase = true)) {
            tickSize = BigDecimal("0.01")
        } else if (trade.setup!!.symbol.equals(Constants.SP500, ignoreCase = true)) {
            tickSize = BigDecimal("0.01")
            lotSize = BigDecimal("0.1")
        } else {
            tickSize = BigDecimal("0.01")
        }

        // Calculate price, stopLoss, and takeProfit
        var price = addTicks(fillPrice, trade.setup!!.tickOffset!!, tickSize)
        var stopLoss = addTicks(fillPrice, trade.setup!!.stop!!, tickSize)
        var takeProfit = addTicks(fillPrice, trade.setup!!.limit!!, tickSize)

        // Apply modifiers if present
        val modifiers = setupModifierRepository.findModifiersBySetupId(trade.setup!!.id!!)
        if (modifiers.isNotEmpty()) {
            // Multiply the price, stopLoss, and takeProfit by all modifier values
            for (modifier in modifiers) {

                if(modifier.modifierName != "ATR") {
                    throw IllegalArgumentException("Unsupported modifier name: ${modifier.modifierName}")
                }

                val priceAdjustmentModifier = modifierRepository.findBySymbolAndModifierNameAndType(
                    symbol = trade.setup!!.symbol!!, modifierName = "ATR-WEIGHTING", type = "adjustment"
                )

                if (priceAdjustmentModifier != null) {

                    val fixedModifierValue =
                        modifier.modifierValue.divide(priceAdjustmentModifier.modifierValue, 10, RoundingMode.HALF_UP)

                    val scale = price.scale()

                    price = price.multiply(fixedModifierValue).setScale(scale, RoundingMode.HALF_EVEN) // BigDecimal multiplication
                    stopLoss = stopLoss.multiply(fixedModifierValue).setScale(scale, RoundingMode.HALF_EVEN) // BigDecimal multiplication
                    takeProfit = takeProfit.multiply(fixedModifierValue).setScale(scale, RoundingMode.HALF_EVEN) // BigDecimal multiplication

                } else {
                    throw IllegalArgumentException("Price adjustment modifier not found for symbol: ${trade.setup!!.symbol}")
                }
            }
        }

        val magic = trade.id
        commandService.openOrder(
            Order(
                symbol = trade.setup!!.symbol,
                orderType = orderType,
                lots = lotSize.toDouble(), // Converting BigDecimal to Double
                price = price,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                magic = magic,
                expiration = timeHelper.addSecondsToCurrentTime(trade.setup!!.outOfTime!!.toLong()),
                comment = "${trade.targetPlaceDateTime} ${trade.setup!!.concatenateFields()}",
            ), accountSetupGroupsDto
        )

        trade.status = Status.ORDER_SENT
        trade.lastUpdatedDateTime = ZonedDateTime.now(clock)
        slackClient.sendSlackNotification("Order placed: " + trade.setup!!.concatenateFields())
        return trade
    }

    fun closeTrades(
        symbol: String, accountSetupGroups: AccountSetupGroupsDto
    ) {
        tradeRepository.findByAccountSetupGroupsSymbolAndStatus(accountSetupGroups.id!!, symbol, Status.FILLED.name)
            .stream().filter { trade: Trade -> trade.setup!!.name != MANUAL_SETUP_NAME }.filter { trade: Trade ->

                val closeDateTime = trade.targetPlaceDateTime!!.plusHours(trade.setup!!.tradeDuration!!.toLong())
                val currentDateTime = ZonedDateTime.now(clock)
                closeDateTime.isBefore(currentDateTime)
            }.map { t -> tradeMapper.toDto(t) }.forEach { trade ->
                closeTrade(trade, accountSetupGroups)
            }
    }

    fun closeTrade(
        tradeDto: TradeDto, accountSetupGroups: AccountSetupGroupsDto
    ) {

        val trade = tradeMapper.toEntity(tradeDto, clock)

        commandService.closeOrdersByMagic(trade.id!!, accountSetupGroups)
        trade.status = Status.CLOSED_BY_MAGIC_SENT
        trade.lastUpdatedDateTime = ZonedDateTime.now(clock)
        //trade.closedDateTime = ZonedDateTime.now()
        tradeRepository.save(trade)
        slackClient.sendSlackNotification("Order closed by magic: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
    }

    fun closeTradesOnStanceChange(
        tradingStanceDto: UpdateTradingStanceDto,
        accountSetupGroups: AccountSetupGroupsDto,
        statusToFind: Status,
        closingStatus: Status
    ): List<Trade> {
        return tradeRepository.findByAccountSetupGroupsSymbolAndStatus(
            accountSetupGroups.id!!, tradingStanceDto.symbol, statusToFind.name
        ).stream().filter { trade: Trade -> trade.setup!!.direction != tradingStanceDto.direction }
            .map { trade: Trade ->
                if (trade.status == Status.FILLED || trade.status == Status.PLACED_IN_MT) {
                    commandService.closeOrdersByMagic(trade.id!!, accountSetupGroups)
                }
                trade.status = closingStatus
                trade.lastUpdatedDateTime = ZonedDateTime.now(clock)
                //trade.closedDateTime = ZonedDateTime.now()
                tradeRepository.save(trade)
                slackClient.sendSlackNotification("Order closed by tradingStanceChange: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
                trade
            }.toList()
    }


    fun placeTrade(tradeInfo: TradeInfo, metatraderId: Long, trade: TradeDto, status: Status) {

        if (trade.status == Status.PENDING || trade.status == Status.ORDER_SENT) {
            trade.apply {
                val tradeTime = ZonedDateTime.of(tradeInfo.openTime, ZoneId.of("Europe/Zurich"))
                if (status == Status.FILLED) {
                    filledPrice = tradeInfo.openPrice
                    filledDateTime = tradeTime
                } else {
                    placedPrice = tradeInfo.openPrice
                    placedDateTime = tradeTime
                }
                this.status = status
                lastUpdatedDateTime = ZonedDateTime.now(clock)
                this.metatraderId = metatraderId
            }.also {
                save(it)
                slackClient.sendSlackNotification("Order placed in MT: $it")
            }
        }
    }

    fun onClosedTrade(tradeInfo: TradeInfo, metatraderId: Long) {
        val trade = tradeRepository.findByIdOrNull(tradeInfo.magic) ?: tradeRepository.findByMetatraderId(metatraderId)

        trade?.let { onClosedTrade(tradeInfo, it) } ?: logger.warn("Trade not found: $tradeInfo")
    }

    private fun onClosedTrade(tradeInfo: TradeInfo, trade: Trade) {

        val closingStatus =

            if (trade.status == Status.CLOSED_BY_MAGIC_SENT) {
                Status.CLOSED_BY_TIME
            } else if (tradeInfo.type.equals("buy") || tradeInfo.type.equals("sell")) {
                Status.CLOSED_BY_USER
            } else if (tradeInfo.type.equals("buylimit") || tradeInfo.type.equals("selllimit")) {
                Status.OUT_OF_TIME
            } else {
                if (trade.status == Status.CLOSED_BY_STANCE) {
                    Status.CLOSED_BY_STANCE
                } else {
                    Status.CLOSED_BY_USER
                }
            }

        trade.apply {
            status = closingStatus
            closedPrice = tradeInfo.takeProfit
            closedDateTime = ZonedDateTime.now(clock)
            profit = tradeInfo.profitAndLoss
            lastUpdatedDateTime = ZonedDateTime.now(clock)
        }.also { tradeRepository.save(it) }

        slackClient.sendSlackNotification("Order closed: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
    }

    fun deleteTradesByAccountName(name: String): Int {
        return tradeRepository.deleteByAccountName(name)
    }

    fun findByAccountName(name: String, sort: Sort): List<TradeDto> {
        return tradeRepository.findByAccount_Name(name, sort).map { tradeMapper.toDto(it) }
    }

    fun findBySetupGroupsName(name: String): List<TradeDto> {
        return tradeRepository.findBySetupGroupsName(name).map { tradeMapper.toDto(it) }
    }

    fun findByMetatraderId(metatraderId: Long): TradeDto? {
        return tradeRepository.findByMetatraderId(metatraderId)?.let { tradeMapper.toDto(it) }
    }

//    fun closeTrades(accountSetupGroups: AccountSetupGroupsDto, symbol: String): Int {
//
//        val openStatuses = setOf(Status.FILLED, Status.PENDING, Status.ORDER_SENT, Status.PLACED_IN_MT)
//
//        val tradesToClose =
//            tradeRepository.findByAccountSetupGroupsAndSymbol(accountSetupGroups.id!!, symbol)
//
//        tradesToClose.filter { t -> openStatuses.contains(t.status) }.forEach { trade ->
//            trade.apply {
//                status = Status.CLOSED_BY_STANCE
//                closedDateTime = ZonedDateTime.now(clock)
//                lastUpdatedDateTime = ZonedDateTime.now(clock)
//            }.also { tradeRepository.save(it) }
//
//            slackClient.sendSlackNotification("Order closed: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")
//        }
//
//        return tradesToClose.size
//
//    }

    companion object {
        fun addTicks(initialPrice: BigDecimal, ticksToAdd: Int, tickSize: BigDecimal): BigDecimal {
            val ticks = tickSize.multiply(BigDecimal.valueOf(ticksToAdd.toLong()))
            return initialPrice.add(ticks)
        }
    }
}
