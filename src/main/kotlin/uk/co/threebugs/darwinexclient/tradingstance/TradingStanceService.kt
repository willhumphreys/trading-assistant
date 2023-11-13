package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.data.repository.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*

@Service
class TradingStanceService(
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
    private val accountSetupGroupsService: AccountSetupGroupsService
) {

    fun findAll(pageable: Pageable): Page<TradingStanceDto> {
        return this.tradingStanceRepository.findAll(pageable)
            .map(tradingStanceMapper::toDto)
    }

    fun findAllByAccountSetupGroupsNameWithSetupCount(groupName: String?, pageable: Pageable): Page<TradingStanceInfo> {
        return this.tradingStanceRepository.findAllByAccountSetupGroupsNameWithSetupCount(groupName, pageable)
    }

    fun updateTradingStance(id: Int, tradingStanceDto: UpdateTradingStanceDto): TradingStanceDto {

        val accountSetupGroups = (accountSetupGroupsService.findByName(tradingStanceDto.accountSetupGroupsName)
            ?: throw IllegalArgumentException("AccountSetupGroups with name ${tradingStanceDto.accountSetupGroupsName} not found"))

        val tradingStanceDto = TradingStanceDto(
            id = id,
            symbol = tradingStanceDto.symbol,
            direction = tradingStanceDto.direction,
            accountSetupGroups = accountSetupGroups
        )

        return this.tradingStanceRepository.findByIdOrNull(id)?.let {
            tradingStanceMapper.toEntity(tradingStanceDto, it)
        }?.let {
            tradingStanceRepository.save(it)
        }?.let {
            tradingStanceMapper.toDto(it)
        } ?: throw IllegalArgumentException("TradingStance with id $id not found")
    }
}