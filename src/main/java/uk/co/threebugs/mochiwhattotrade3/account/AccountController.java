package uk.co.threebugs.mochiwhattotrade3.account;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/accounts")
    public List<AccountDto> findAll() {
        return accountService.findAll();
    }
}
