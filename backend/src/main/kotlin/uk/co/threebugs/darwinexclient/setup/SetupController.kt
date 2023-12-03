package uk.co.threebugs.darwinexclient.setup

import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/setups")
class SetupController(private val setupService: SetupService) {

    @DeleteMapping("/byAccountName/{name}")
    fun deleteSetupsByAccountName(@PathVariable name: String): ResponseEntity<Int> {
        return try {
            val rowsDeleted = setupService.deleteSetupsByAccountName(name)
            ResponseEntity.ok(rowsDeleted)
        } catch (e: Exception) {
            // Log the error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping
    fun getSetups(): ResponseEntity<List<SetupDto>> {
        return ResponseEntity.ok(setupService.findAll())
    }
}