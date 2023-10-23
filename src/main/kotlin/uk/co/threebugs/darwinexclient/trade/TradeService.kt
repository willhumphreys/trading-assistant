package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.SlackClient
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroups
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroupsRepository
import uk.co.threebugs.darwinexclient.metatrader.Client
import uk.co.threebugs.darwinexclient.metatrader.Order
import uk.co.threebugs.darwinexclient.metatrader.TradeInfo
import uk.co.threebugs.darwinexclient.setup.SetupFileRepository
import uk.co.threebugs.darwinexclient.setup.SetupRepository
import uk.co.threebugs.darwinexclient.utils.Constants
import uk.co.threebugs.darwinexclient.utils.TimeHelper
import uk.co.threebugs.darwinexclient.utils.logger
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.function.Consumer

@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val setupRepository: SetupRepository,
    private val accountSetupGroupsRepository: AccountSetupGroupsRepository,
    private val tradeMapper: TradeMapper,
    private val timeHelper: TimeHelper,
    private val slackClient: SlackClient
) {
    fun findById(id: Int): TradeDto? {
        return tradeRepository.findByIdOrNull(id)?.let { tradeMapper.toDto(it) }
    }


    fun save(tradeDto: TradeDto): TradeDto {
        val setup = setupRepository.findByIdOrNull(tradeDto.setup!!.id)
            ?: throw IllegalArgumentException("Setup not found for ID: ${tradeDto.setup!!.id}")

        var record = tradeMapper.toEntity(tradeDto, setup)
        record = tradeRepository.save(record)
        return tradeMapper.toDto(record)
    }

    fun deleteById(id: Int) {
        tradeRepository.deleteById(id)
    }

    fun findAll(): List<TradeDto> {
        return tradeRepository.findAll().filterNotNull().map { tradeMapper.toDto(it) }
    }

    fun findTrades(exampleRecord: TradeDto, sort: Sort): List<TradeDto?> {
        val example = Example.of(tradeMapper.toEntity(exampleRecord))

        return tradeRepository.findAll(example, sort).map { tradeMapper.toDto(it) }
    }

    fun createTradesToPlaceFromEnabledSetups(symbol: String, account: Account) {
        val accountSetupGroups = accountSetupGroupsRepository.findByAccount(account)
        accountSetupGroups.forEach { accountSetupGroup ->
            createAndPlaceTradesForEnabledSetupsInSetupGroup(symbol, account, accountSetupGroup)
        }
    }

    private fun createAndPlaceTradesForEnabledSetupsInSetupGroup(
        symbol: String,
        account: Account,
        accountSetupGroup: AccountSetupGroups
    ) {
        val setups = setupRepository.findEnabledSetups(symbol, accountSetupGroup.setupGroups!!)
        setups.forEach { setup ->
            val targetPlaceTime: ZonedDateTime =
                SetupFileRepository.getNextEventTime(setup.dayOfWeek!!, setup.hourOfDay!!)
            val existingTrade =
                tradeRepository.findBySetupAndTargetPlaceDateTimeAndAccount(setup, targetPlaceTime, account)
            if (existingTrade == null) {
                val trade = tradeMapper.toEntity(setup, targetPlaceTime, account).apply {
                    status = Status.PENDING
                }
                tradeRepository.save(trade)
                slackClient.sendSlackNotification(trade.newTradeMessage)
            }
        }
    }

    fun placeTrades(dwx: Client, symbol: String, bid: BigDecimal, ask: BigDecimal, account: Account) {
        tradeRepository.findByStatusAndSetup_SymbolAndAccount(Status.PENDING, symbol, account)
            .forEach(Consumer { trade: Trade ->
                val now = ZonedDateTime.now(ZoneOffset.UTC)
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

    fun closeTradesAtTime(dwx: Client, symbol: String, account: Account) {
        tradeRepository.findByStatusAndSetup_SymbolAndAccount(Status.FILLED, symbol, account).stream()
            .filter { record: Trade ->
                record.targetPlaceDateTime!!.plusHours(record.setup!!.tradeDuration!!.toLong())
                    .isBefore(ZonedDateTime.now(ZoneOffset.UTC))
            }.forEach { trade: Trade ->
                dwx.closeOrdersByMagic(trade.id!!)
                trade.status = Status.CLOSED_BY_TIME
                trade.closedDateTime = ZonedDateTime.now()
                tradeRepository.save(trade)
                slackClient.sendSlackNotification("Order closed: " + trade.setup!!.rank + " " + trade.setup!!.symbol + " " + (if (trade.setup!!.isLong) "LONG" else "SHORT") + " " + trade.profit)
            }
    }

    fun fillTrade(tradeInfo: TradeInfo, metatraderId: Int) {
        tradeRepository.findByIdOrNull(tradeInfo.magic)
            ?.takeIf { it.status == Status.PENDING || it.status == Status.ORDER_SENT }
            ?.apply {
                placedPrice = tradeInfo.openPrice
                placedDateTime = ZonedDateTime.of(tradeInfo.openTime, ZoneId.of("Europe/Zurich"))
                status = Status.PLACED_IN_MT
                this.metatraderId = metatraderId
            }
            ?.also {
                tradeRepository.save(it)
                slackClient.sendSlackNotification("Order placed in MT: $it")
            } ?: logger.warn("Trade not found or status not suitable: $tradeInfo")
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
        trade.apply {
            status = Status.CLOSED_BY_USER
            closedPrice = tradeInfo.takeProfit
            closedDateTime = ZonedDateTime.now()
            profit = tradeInfo.profitAndLoss
        }.also { tradeRepository.save(it) }

        slackClient.sendSlackNotification("Order closed: ${trade.setup!!.rank} ${trade.setup!!.symbol} ${trade.setup!!.direction} ${trade.profit}")

    }

    companion object {
        fun addTicks(initialPrice: BigDecimal, ticksToAdd: Int, tickSize: BigDecimal): BigDecimal {
            val ticks = tickSize.multiply(BigDecimal.valueOf(ticksToAdd.toLong()))
            return initialPrice.add(ticks)
        }
    }
}
