package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.stereotype.*

@Service
class TradingStanceService(
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
) {

    fun findAll(sort: Sort): List<TradingStanceDto> {
        return this.tradingStanceRepository.findAll(sort).map { tradingStance ->
            tradingStanceMapper.toDto(tradingStance)
        }.toList()
    }

    fun findByAccountSetupGroupsName(accountSetupGroupsName: String, sort: Sort): List<TradingStanceDto> {
        return this.tradingStanceRepository.findByAccountSetupGroups_Name(accountSetupGroupsName, sort)
            .map { tradingStance ->
                tradingStanceMapper.toDto(tradingStance)
        }.toList()
    }
}