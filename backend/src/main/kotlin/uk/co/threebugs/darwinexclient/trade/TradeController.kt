package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.domain.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import uk.co.threebugs.darwinexclient.search.*

@RestController
@RequestMapping("/trades")
class TradeController(
    private val tradeService: TradeService
) {
//    @GetMapping("")
//    fun findAll(): List<TradeDto> {
//        return tradeService.findAll()
//    }

    @GetMapping("")
    fun getTrades(
        @RequestParam(name = "accountName", required = false) accountName: String?,
        @RequestParam(name = "setupGroupsName", required = false) setupGroupsName: String?,
        @RequestParam(name = "sortColumn", required = false) sortColumn: String?,
        @RequestParam(name = "sortDirection", required = false) sortDirection: Sort.Direction?
    ): ResponseEntity<List<TradeDto>> {
        var sort = Sort.unsorted()
        sortColumn?.let { column ->
            sortDirection?.let { direction ->
                sort = Sort.by(direction, column)
            }
        }

        return when {
            accountName != null -> ResponseEntity.ok(tradeService.findByAccountName(accountName, sort))
            setupGroupsName != null -> ResponseEntity.ok(tradeService.findBySetupGroupsName(setupGroupsName))
            else -> ResponseEntity.ok(tradeService.findAll(sort))
        }
    }

    @PostMapping("/searchByExample")
    fun findWithExample(
        @RequestBody exampleRecord: TradeSearchDto,
        @RequestParam(name = "sortColumn", required = false) sortColumn: String?,
        @RequestParam(name = "sortDirection", required = false) sortDirection: Sort.Direction?
    ): List<TradeSearchDto> {
        var sort = Sort.unsorted()
        if (sortColumn != null && sortDirection != null) {
            sort = Sort.by(sortDirection, sortColumn)
        }

        return tradeService.findTrades(exampleRecord, sort)
    }

    @DeleteMapping("")
    fun deleteTrades(
        @RequestParam(name = "accountName", required = false) accountName: String?,
        @RequestParam(name = "setupGroupsName", required = false) setupGroupsName: String?
    ): ResponseEntity<Any> {
        return when {
            accountName != null -> {
                try {
                    val rowsDeleted = tradeService.deleteTradesByAccountName(accountName)
                    ResponseEntity.ok(rowsDeleted)
                } catch (e: Exception) {
                    // Log the error
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            }

            setupGroupsName != null -> {
                try {
                    val rowsDeleted = tradeService.deleteTradesBySetupGroupsName(setupGroupsName)
                    ResponseEntity.ok(rowsDeleted)
                } catch (e: Exception) {
                    // Log the error
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            }

            else -> ResponseEntity.badRequest().body("An attribute for deletion must be provided.")
        }
    }

    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: Int) {
        tradeService.deleteById(id)
    }

//    @DeleteMapping("/bySetupGroupsName/{name}")
//    fun deleteTradesBySetupGroupsName(@PathVariable name: String): ResponseEntity<Int> {
//        return try {
//            val rowsDeleted = tradeService.deleteTradesBySetupGroupsName(name)
//            ResponseEntity.ok(rowsDeleted)
//        } catch (e: Exception) {
//            // Log the error
//            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
//        }
//    }
//
//    @DeleteMapping("/byAccountName/{name}")
//    fun deleteTradesByAccountName(@PathVariable name: String): ResponseEntity<Int> {
//        return try {
//            val rowsDeleted = tradeService.deleteTradesByAccountName(name)
//            ResponseEntity.ok(rowsDeleted)
//        } catch (e: Exception) {
//            // Log the error
//            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
//        }
//    }

//    @GetMapping("/byAccountName/{name}")
//    fun getTradesByAccountName(@PathVariable name: String): ResponseEntity<List<TradeDto>> {
//        return try {
//            return ResponseEntity.ok().body(tradeService.findByAccountName(name))
//        } catch (e: Exception) {
//            // Log the error
//            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
//        }
//    }
//
//    @GetMapping("/bySetupGroupsName/{name}")
//    fun getTradesBySetupGroupsName(@PathVariable name: String): ResponseEntity<List<TradeDto>> {
//        return try {
//            return ResponseEntity.ok().body(tradeService.findBySetupGroupsName(name))
//        } catch (e: Exception) {
//            // Log the error
//            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
//        }
//    }
}
