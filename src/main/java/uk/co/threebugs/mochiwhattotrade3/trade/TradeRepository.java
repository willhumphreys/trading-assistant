package uk.co.threebugs.mochiwhattotrade3.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import uk.co.threebugs.mochiwhattotrade3.Type;
import uk.co.threebugs.mochiwhattotrade3.account.Account;
import uk.co.threebugs.mochiwhattotrade3.setup.Setup;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Integer>, QueryByExampleExecutor<Trade> {

    List<Trade> findByTypeAndSetup_SymbolAndAccount(Type type, String symbol, Account account);

    Optional<Trade> findBySetupAndPlacedDateTimeAndAccount(Setup setup, ZonedDateTime placedDateTime, Account account);

}
