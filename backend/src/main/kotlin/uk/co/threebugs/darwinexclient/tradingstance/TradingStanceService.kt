package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.data.repository.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.trade.*

@Service
class TradingStanceService(
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
    private val accountSetupGroupsService: AccountSetupGroupsService,
    private val tradeService: TradeService,
    private val accountSetupGroupsMapper: AccountSetupGroupsMapper
) {

    fun findAll(pageable: Pageable): Page<TradingStanceDto> {
        return this.tradingStanceRepository.findAll(pageable)
            .map(tradingStanceMapper::toDto)
    }

    fun findAllByAccountSetupGroupsNameWithSetupCount(groupName: String?, pageable: Pageable): Page<TradingStanceInfo> {
        return this.tradingStanceRepository.findAllByAccountSetupGroupsNameWithSetupCount(groupName, pageable)
    }

    fun updateTradingStance(id: Int, tradingStanceDto: UpdateTradingStanceDto): TradingStanceDto {

        val accountSetupGroups = accountSetupGroupsService.findByName(tradingStanceDto.accountSetupGroupsName)
            ?: throw IllegalArgumentException("AccountSetupGroups with name ${tradingStanceDto.accountSetupGroupsName} not found")

        val existingTradingStance = tradingStanceRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("TradingStance with id $id not found")

        if (existingTradingStance.direction == tradingStanceDto.direction) {
            throw IllegalArgumentException("No change in direction for TradingStance with id $id")
        }

        // Proceed with updating the trading stance
        val updatedEntity = tradingStanceMapper.toEntity(
            TradingStanceDto(
                id = id,
                symbol = tradingStanceDto.symbol,
                direction = tradingStanceDto.direction,
                accountSetupGroups = accountSetupGroups
            ), existingTradingStance
        )

        val savedEntity = tradingStanceRepository.save(updatedEntity)

        tradeService.closeTrades(tradingStanceDto.symbol, accountSetupGroupsMapper.toEntity(accountSetupGroups))

        return tradingStanceMapper.toDto(savedEntity)
    }
}