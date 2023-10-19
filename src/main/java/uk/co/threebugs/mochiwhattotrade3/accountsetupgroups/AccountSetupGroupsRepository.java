package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import uk.co.threebugs.mochiwhattotrade3.account.Account;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountSetupGroupsRepository extends JpaRepository<AccountSetupGroups, Integer>, QueryByExampleExecutor<AccountSetupGroups> {
    Optional<AccountSetupGroups> findByName(String name);

    List<AccountSetupGroups> findByAccount(Account account);
}
