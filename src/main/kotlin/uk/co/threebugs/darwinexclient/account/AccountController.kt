package uk.co.threebugs.darwinexclient.account

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountController(private val accountService: AccountService) {

        @GetMapping("/accounts")
    fun findAll(): List<AccountDto> {
        return accountService.findAll()
    }
}
