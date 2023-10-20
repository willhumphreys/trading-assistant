package uk.co.threebugs.mochiwhattotrade3.setupgroup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups
import java.util.*

interface SetupGroupRepository : JpaRepository<SetupGroup, Int>, QueryByExampleExecutor<SetupGroup> {
    fun findByPath(path: String): Optional<SetupGroup>
    fun findBySetupGroups(setupGroups: SetupGroups): List<SetupGroup>
    fun findBySetupGroups_Id(setupGroupsId: Int): List<SetupGroup>
}
