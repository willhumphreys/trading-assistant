package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/trades")
class TradeController (
    private val tradeService: TradeService
)
{
    @GetMapping("")
    fun findAll(): List<TradeDto> {
        return tradeService.findAll()
    }

    @PostMapping("/searchByExample")
    fun findWithExample(
        @RequestBody exampleRecord: TradeDto,
            @RequestParam(name = "sortColumn", required = false) sortColumn: String?,
            @RequestParam(name = "sortDirection", required = false) sortDirection: Sort.Direction?): List<TradeDto?> {
        var sort = Sort.unsorted()
        if (sortColumn != null && sortDirection != null) {
            sort = Sort.by(sortDirection, sortColumn)
        }

        return tradeService.findTrades(exampleRecord, sort)
    }

    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: Int) {
        tradeService.deleteById(id)
    }

    @DeleteMapping("/bySetupGroupName/{name}")
    fun deleteTradesBySetupGroupName(@PathVariable name: String): ResponseEntity<Unit> {
        return try {
            tradeService.deleteTradesBySetupGroupName(name)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            // Log the error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/byAccountName/{name}")
    fun deleteTradesByAccountName(@PathVariable name: String): ResponseEntity<Int> {
        return try {
            val rowsDeleted = tradeService.deleteTradesByAccountName(name)
            ResponseEntity.ok(rowsDeleted)
        } catch (e: Exception) {
            // Log the error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/byAccountName/{name}")
    fun getTradesByAccountName(@PathVariable name: String): ResponseEntity<List<TradeDto>> {
        return try {
            return ResponseEntity.ok().body(tradeService.findByAccountName(name))
        } catch (e: Exception) {
            // Log the error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
