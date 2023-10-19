package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.Optional;

public interface SetupGroupsRepository extends JpaRepository<SetupGroups, Integer>, QueryByExampleExecutor<SetupGroups> {

    Optional<SetupGroups> findByScriptsDirectory(String scriptsDirectory);

    Optional<SetupGroups> findByName(String name);

}
