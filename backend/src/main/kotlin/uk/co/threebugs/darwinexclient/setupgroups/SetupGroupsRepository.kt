package uk.co.threebugs.darwinexclient.setupgroups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import java.util.*

interface SetupGroupsRepository : JpaRepository<SetupGroups, Int>, QueryByExampleExecutor<SetupGroups> {
    fun findByScriptsDirectory(scriptsDirectory: String): Optional<SetupGroups>
    fun findByName(name: String): Optional<SetupGroups>
}
