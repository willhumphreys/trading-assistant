package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsDto;

@Value
@Builder
@Jacksonized
public class AccountSetupGroupsDto {

    Integer id;
    String name;
    AccountDto account;
    SetupGroupsDto setupGroups;
}
