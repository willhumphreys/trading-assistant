package uk.co.threebugs.mochiwhattotrade3.setupgroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups;

import java.util.List;
import java.util.Optional;

public interface SetupGroupRepository extends JpaRepository<SetupGroup, Integer>, QueryByExampleExecutor<SetupGroup> {


    Optional<SetupGroup> findByPath(String path);

    List<SetupGroup> findBySetupGroups(SetupGroups setupGroups);

    List<SetupGroup> findBySetupGroups_Id(Integer setupGroupsId);
}
