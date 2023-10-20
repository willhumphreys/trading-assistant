package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.darwinexclient.Type
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.setup.Setup
import java.time.ZonedDateTime

@Repository
interface TradeRepository : JpaRepository<Trade, Int>, QueryByExampleExecutor<Trade> {
    fun findByTypeAndSetup_SymbolAndAccount(type: Type, symbol: String, account: Account): List<Trade>
    fun findBySetupAndPlacedDateTimeAndAccount(setup: Setup, placedDateTime: ZonedDateTime, account: Account): Trade?
}
