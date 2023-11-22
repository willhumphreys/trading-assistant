package uk.co.threebugs.darwinexclient.accountsetupgroups

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountSetupGroupsController(private val accountSetupGroupsService: AccountSetupGroupsService) {
    @GetMapping("/accountSetupGroups")
    fun findAll(): List<AccountSetupGroupsDto> {
        return accountSetupGroupsService.findAll()
    }
}
