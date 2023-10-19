package uk.co.threebugs.mochiwhattotrade3.setup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups;

import java.util.List;
import java.util.Optional;

@Repository
public interface SetupRepository extends JpaRepository<Setup, Integer>, QueryByExampleExecutor<Setup> {

    Optional<Setup> findBySymbolAndRankAndSetupGroup(String symbol, int rank, SetupGroup setupGroup);


    @Query("SELECT s FROM Setup s WHERE s.setupGroup.setupGroups = :setupGroups and s.setupGroup.enabled = true and s.symbol = :symbol")
    List<Setup> findEnabledSetups(String symbol, SetupGroups setupGroups);
}
