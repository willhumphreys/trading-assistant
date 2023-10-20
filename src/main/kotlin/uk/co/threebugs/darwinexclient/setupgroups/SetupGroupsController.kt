package uk.co.threebugs.darwinexclient.setupgroups

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SetupGroupsController (
    private val setupGroupsService: SetupGroupsService
)
{
    @GetMapping("/setupGroups")
    fun findAll(): List<SetupGroupsDto> {
        return setupGroupsService.findAll()
    }
}
