package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.mochiwhattotrade3.account.Account

@Repository
interface AccountSetupGroupsRepository : JpaRepository<AccountSetupGroups, Int>, QueryByExampleExecutor<AccountSetupGroups> {
    fun findByName(name: String): AccountSetupGroups?
    fun findByAccount(account: Account): List<AccountSetupGroups>
}
