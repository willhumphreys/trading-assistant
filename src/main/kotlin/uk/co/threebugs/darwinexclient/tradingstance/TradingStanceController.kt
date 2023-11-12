package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.web.bind.annotation.*

@RestController
class TradingStanceController(private val tradingStanceService: TradingStanceService) {

    @GetMapping("/trading-stances")
    fun getTradingStances(pageable: Pageable): Page<TradingStanceDto> {
        return tradingStanceService.findAll(pageable)
    }

    @GetMapping("/trading-stances-with-setup-count")
    fun getTradingStancesWithSetupCount(
        @RequestParam(name = "accountSetupGroupsName", required = false) accountSetupGroupsName: String?,
        pageable: Pageable
    ): Page<TradingStanceInfo> {
        return tradingStanceService.findAllByAccountSetupGroupsNameWithSetupCount(accountSetupGroupsName, pageable)
    }
}