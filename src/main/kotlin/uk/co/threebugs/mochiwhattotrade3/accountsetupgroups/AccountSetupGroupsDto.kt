package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups

import uk.co.threebugs.mochiwhattotrade3.account.AccountDto
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroupsDto


data class AccountSetupGroupsDto (
    var id: Int? = null,
    var name: String? = null,
    var account: AccountDto? = null,
    var setupGroups: SetupGroupsDto? = null
)

