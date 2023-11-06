package uk.co.threebugs.darwinexclient.setupgroup

import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/setupGroup")
class SetupGroupController(
    private val setupGroupService: SetupGroupService
) {

    @GetMapping()
    fun findAll(): ResponseEntity<List<SetupGroupDto>> {
        return ResponseEntity.ok(setupGroupService.findAll())
    }
}