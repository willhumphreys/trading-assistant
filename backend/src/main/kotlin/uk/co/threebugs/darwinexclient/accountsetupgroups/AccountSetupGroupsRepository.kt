package uk.co.threebugs.darwinexclient.accountsetupgroups

import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*

@Repository
interface AccountSetupGroupsRepository : JpaRepository<AccountSetupGroups, Int>,
    QueryByExampleExecutor<AccountSetupGroups> {
    fun findByName(name: String): AccountSetupGroups?
}
