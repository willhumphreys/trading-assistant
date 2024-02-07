package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.data.repository.*
import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*
import uk.co.threebugs.darwinexclient.Status.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import uk.co.threebugs.darwinexclient.trade.*

@Transactional
@Service
class TradingStanceService(
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val tradeService: TradeService
) {

    fun findAll(pageable: Pageable): Page<TradingStanceDtoIn> {
        return this.tradingStanceRepository.findAll(pageable)
            .map(tradingStanceMapper::toDto)
    }

    fun findAllByAccountSetupGroupsNameWithSetupCount(groupName: String?, pageable: Pageable): Page<TradingStanceInfo> {
        return this.tradingStanceRepository.findAllByAccountSetupGroupsNameWithSetupCount(groupName, pageable)
    }

    fun updateTradingStance(id: Int, tradingStanceDto: UpdateTradingStanceDto): TradingStanceDtoOut {

        val accountSetupGroups = accountSetupGroupsService.findByName(tradingStanceDto.accountSetupGroupsName)
            ?: throw IllegalArgumentException("AccountSetupGroups with name ${tradingStanceDto.accountSetupGroupsName} not found")

        val existingTradingStance = tradingStanceRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("TradingStance with id $id not found")

        if (existingTradingStance.direction == tradingStanceDto.direction) {
            throw IllegalArgumentException("No change in direction for TradingStance with id $id")
        }

        val updatedEntity = tradingStanceMapper.toEntity(
            TradingStanceDtoIn(
                id = id,
                symbol = tradingStanceDto.symbol,
                direction = tradingStanceDto.direction,
                accountSetupGroups = accountSetupGroups
            ), existingTradingStance
        )

        val savedEntity = tradingStanceRepository.save(updatedEntity)

        // Unless the trading stance is set to BOTH, we need to close any open trades
        if (tradingStanceDto.direction != Direction.BOTH) {

            val trades = listOf(
                PENDING to CANCELLED_BY_STANCE,
                ORDER_SENT to CANCELLED_BY_STANCE,
                PLACED_IN_MT to CANCELLED_BY_STANCE,
                FILLED to CLOSED_BY_STANCE
            ).map { (status, closureReason) ->
                tradeService.closeTradesOnStanceChange(
                    tradingStanceDto.symbol,
                    accountSetupGroups,
                    status,
                    closureReason
                )
            }.flatten().toList()
            return tradingStanceMapper.toDto(savedEntity, trades)
        }
        return tradingStanceMapper.toDto(savedEntity, listOf())
    }

    fun findBySymbolAndAccountSetupGroupsName(symbol: String, accountSetupGroupsName: String): TradingStanceDtoIn {

        val tradingStance =
            (tradingStanceRepository.findBySymbolAndAccountSetupGroups_Name(symbol, accountSetupGroupsName)
                ?: throw IllegalArgumentException("TradingStance with symbol $symbol and accountSetupGroupsName $accountSetupGroupsName not found"))

        return tradingStanceMapper.toDto(tradingStance)

    }
}