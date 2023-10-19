package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class AccountSetupGroupsController {

    private final AccountSetupGroupsService accountSetupGroupsService;

    @GetMapping("/accountSetupGroups")
    public List<AccountSetupGroupsDto> findAll() {
        return accountSetupGroupsService.findAll();
    }
}
