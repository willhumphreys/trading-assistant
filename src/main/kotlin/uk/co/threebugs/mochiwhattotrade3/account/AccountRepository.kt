package uk.co.threebugs.mochiwhattotrade3.account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, Int>, QueryByExampleExecutor<Account> {
    fun findByName(name: String): Optional<Account>
}
