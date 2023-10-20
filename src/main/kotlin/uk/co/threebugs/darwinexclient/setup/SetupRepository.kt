package uk.co.threebugs.darwinexclient.setup

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroups

@Repository
interface SetupRepository : JpaRepository<Setup, Int>, QueryByExampleExecutor<Setup> {
    fun findBySymbolAndRankAndSetupGroup(symbol: String, rank: Int, setupGroup: SetupGroup): Setup?

    @Query("SELECT s FROM Setup s WHERE s.setupGroup.setupGroups = :setupGroups and s.setupGroup.enabled = true and s.symbol = :symbol")
    fun findEnabledSetups(symbol: String, setupGroups: SetupGroups): List<Setup>
}
