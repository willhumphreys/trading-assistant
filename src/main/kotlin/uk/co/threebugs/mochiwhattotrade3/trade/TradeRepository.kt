package uk.co.threebugs.mochiwhattotrade3.trade

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.mochiwhattotrade3.Type
import uk.co.threebugs.mochiwhattotrade3.account.Account
import uk.co.threebugs.mochiwhattotrade3.setup.Setup
import java.time.ZonedDateTime

@Repository
interface TradeRepository : JpaRepository<Trade, Int>, QueryByExampleExecutor<Trade> {
    fun findByTypeAndSetup_SymbolAndAccount(type: Type, symbol: String, account: Account): List<Trade>
    fun findBySetupAndPlacedDateTimeAndAccount(setup: Setup, placedDateTime: ZonedDateTime, account: Account): Trade?
}
