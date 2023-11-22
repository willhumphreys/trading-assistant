package uk.co.threebugs.darwinexclient.setupgroups

import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/setupGroups")
class SetupGroupsController(
    private val setupGroupsService: SetupGroupsService
) {
    @GetMapping()
    fun findAll(): ResponseEntity<List<SetupGroupsDto>> {
        return ResponseEntity.ok(setupGroupsService.findAll())
    }

}
