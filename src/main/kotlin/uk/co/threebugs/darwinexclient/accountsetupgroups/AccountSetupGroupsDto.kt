package uk.co.threebugs.darwinexclient.accountsetupgroups

import uk.co.threebugs.darwinexclient.account.AccountDto
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsDto


data class AccountSetupGroupsDto (
    var id: Int? = null,
    var name: String? = null,
    var account: AccountDto? = null,
    var setupGroups: SetupGroupsDto? = null
)

