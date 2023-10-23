package uk.co.threebugs.darwinexclient.trade

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.darwinexclient.Status
import uk.co.threebugs.darwinexclient.account.Account
import uk.co.threebugs.darwinexclient.setup.Setup
import java.time.ZonedDateTime

@Repository
interface TradeRepository : JpaRepository<Trade, Int>, QueryByExampleExecutor<Trade> {
    fun findByStatusAndSetup_SymbolAndAccount(status: Status, symbol: String, account: Account): List<Trade>
    fun findBySetupAndTargetPlaceDateTimeAndAccount(
        setup: Setup,
        placedDateTime: ZonedDateTime,
        account: Account
    ): Trade?
}
