package uk.co.threebugs.darwinexclient.setup

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SetupController(private val setupService: SetupService) {

    @DeleteMapping("/setups/byAccountName/{name}")
    fun deleteSetupsByAccountName(@PathVariable name: String): ResponseEntity<Int> {
        return try {
            val rowsDeleted = setupService.deleteSetupsByAccountName(name)
            ResponseEntity.ok(rowsDeleted)
        } catch (e: Exception) {
            // Log the error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }


}