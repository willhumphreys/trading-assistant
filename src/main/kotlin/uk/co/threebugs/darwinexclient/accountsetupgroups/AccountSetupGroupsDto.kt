package uk.co.threebugs.darwinexclient.accountsetupgroups

import uk.co.threebugs.darwinexclient.account.AccountDto
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroupsDto


data class AccountSetupGroupsDto (
    var id: Int? = null,
    val name: String,
    val account: AccountDto,
    val setupGroups: SetupGroupsDto
)

