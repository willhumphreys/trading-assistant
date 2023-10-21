package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
class TradeController (
    private val tradeService: TradeService
)
{
    @GetMapping("/trades")
    fun findAll(): List<TradeDto> {
        return tradeService.findAll()
    }

    @PostMapping("/trades/searchByExample")
    fun findWithExample(
        @RequestBody exampleRecord: TradeDto,
            @RequestParam(name = "sortColumn", required = false) sortColumn: String?,
            @RequestParam(name = "sortDirection", required = false) sortDirection: Sort.Direction?): List<TradeDto?> {
        var sort = Sort.unsorted()
        if (sortColumn != null && sortDirection != null) {
            sort = Sort.by(sortDirection, sortColumn)
        }

        //val exampleRecord1 = TradeDto()
        return tradeService.findTrades(exampleRecord, sort)
    }
}
