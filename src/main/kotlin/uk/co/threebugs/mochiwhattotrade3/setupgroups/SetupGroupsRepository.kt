package uk.co.threebugs.mochiwhattotrade3.setupgroups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import java.util.*

interface SetupGroupsRepository : JpaRepository<SetupGroups, Int>, QueryByExampleExecutor<SetupGroups> {
    fun findByScriptsDirectory(scriptsDirectory: String): Optional<SetupGroups>
    fun findByName(name: String): Optional<SetupGroups>
}
