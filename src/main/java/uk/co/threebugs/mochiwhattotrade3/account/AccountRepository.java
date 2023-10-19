package uk.co.threebugs.mochiwhattotrade3.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>, QueryByExampleExecutor<Account> {
    Optional<Account> findByName(String name);
}
