package uk.co.threebugs.darwinexclient.setupgroup

import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import uk.co.threebugs.darwinexclient.setupgroups.*

interface SetupGroupRepository : JpaRepository<SetupGroup, Int>, QueryByExampleExecutor<SetupGroup> {
    fun findByPathAndSetupGroups(path: String, setupGroups: SetupGroups): SetupGroup?
    fun findBySetupGroups(setupGroups: SetupGroups): List<SetupGroup>
    fun findBySetupGroups_Id(setupGroupsId: Int): List<SetupGroup>
}
