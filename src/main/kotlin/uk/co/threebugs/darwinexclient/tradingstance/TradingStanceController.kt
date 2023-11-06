package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.web.bind.annotation.*

@RestController
class TradingStanceController(private val tradingStanceService: TradingStanceService) {

    @GetMapping("/trading-stances")
    fun getTradingStances(
        @RequestParam(name = "accountName", required = false) accountName: String?,
        @RequestParam(name = "sortColumn", required = false) sortColumn: String?,
        @RequestParam(name = "sortDirection", required = false) sortDirection: Sort.Direction?
    ): List<TradingStanceDto> {

        var sort = Sort.unsorted()
        sortColumn?.let { column ->
            sortDirection?.let { direction ->
                sort = Sort.by(direction, column)
            }
        }

        return when {
            accountName != null -> tradingStanceService.findByAccountName(accountName, sort)
            else -> tradingStanceService.findAll()
        }


    }

    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: Int) {
        tradingStanceService.deleteById(id)
    }


}