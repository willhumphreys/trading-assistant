package uk.co.threebugs.darwinexclient.setup

import jakarta.transaction.*
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.setupgroup.*

@Repository
interface SetupRepository : JpaRepository<Setup, Int>, QueryByExampleExecutor<Setup> {
    fun findBySymbolAndRankAndSetupGroup(symbol: String, rank: Int, setupGroup: SetupGroup): Setup?

    @Query("SELECT s FROM Setup s WHERE s.setupGroup.setupGroups.name = :setupGroupsName and s.setupGroup.enabled = true and s.symbol = :symbol")
    fun findEnabledSetups(symbol: String, setupGroupsName: String): List<Setup>

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
