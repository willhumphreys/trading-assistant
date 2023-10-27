package uk.co.threebugs.darwinexclient.setup

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroup
import uk.co.threebugs.darwinexclient.setupgroups.SetupGroups

@Repository
interface SetupRepository : JpaRepository<Setup, Int>, QueryByExampleExecutor<Setup> {
    fun findBySymbolAndRankAndSetupGroup(symbol: String, rank: Int, setupGroup: SetupGroup): Setup?

    @Query("SELECT s FROM Setup s WHERE s.setupGroup.setupGroups = :setupGroups and s.setupGroup.enabled = true and s.symbol = :symbol")
    fun findEnabledSetups(symbol: String, setupGroups: SetupGroups): List<Setup>

    @Transactional
    @Modifying
    @Query(
        value = """
    DELETE FROM setup
    WHERE id IN (
        SELECT * FROM (
            SELECT s.id FROM setup s
            JOIN setup_group sg ON s.setup_group_id = sg.id
            JOIN setup_groups sgs ON sg.setup_groups_id = sgs.id
            JOIN account_setup_groups asg ON sgs.id = asg.setup_groups_id
            JOIN account a ON asg.account_id = a.id
            WHERE a.name = :name
        ) AS temp
    )
""", nativeQuery = true
    )
    fun deleteSetupsByAccountName(@Param("name") name: String): Int

}
