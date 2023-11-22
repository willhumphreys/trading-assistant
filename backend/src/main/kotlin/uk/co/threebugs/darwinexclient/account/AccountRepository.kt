package uk.co.threebugs.darwinexclient.account

import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*

@Repository
interface AccountRepository : JpaRepository<Account, Int>, QueryByExampleExecutor<Account> {
    fun findByName(name: String): Account?
}
