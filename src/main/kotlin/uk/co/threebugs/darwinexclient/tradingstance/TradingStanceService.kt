package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.stereotype.*

@Service
class TradingStanceService(
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
) {

    fun findAll(pageable: Pageable): Page<TradingStanceDto> {
        return this.tradingStanceRepository.findAll(pageable)
            .map(tradingStanceMapper::toDto)
    }

    fun findAllByAccountSetupGroupsNameWithSetupCount(groupName: String?, pageable: Pageable): Page<TradingStanceInfo> {
        return this.tradingStanceRepository.findAllByAccountSetupGroupsNameWithSetupCount(groupName, pageable)
    }
}