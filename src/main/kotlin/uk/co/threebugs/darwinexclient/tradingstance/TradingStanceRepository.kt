package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*

@Repository
interface TradingStanceRepository : JpaRepository<TradingStance, Int>, QueryByExampleExecutor<TradingStance> {
    fun findByAccountSetupGroups_Name(name: String, sort: Sort): List<TradingStance>
    fun findBySymbol(symbol: String): TradingStance?

    @Query(
        "SELECT new uk.co.threebugs.darwinexclient.tradingstance.TradingStanceInfo(ts, " +
                "(SELECT COUNT(s.id) FROM Setup s WHERE s.setupGroup.setupGroups.id = asg.setupGroups.id AND s.symbol = ts.symbol AND s.setupGroup.enabled = true), " +
                "(SELECT COUNT(s.id) FROM Setup s WHERE s.setupGroup.setupGroups.id = asg.setupGroups.id AND s.symbol = ts.symbol AND s.setupGroup.enabled = false)) " +
                "FROM TradingStance ts " +
                "JOIN ts.accountSetupGroups asg " +
                "WHERE asg.name = :groupName"
    )
    fun findAllByAccountSetupGroupsNameWithSetupCount(
        @Param("groupName") groupName: String?,
        pageable: Pageable
    ): Page<TradingStanceInfo>

}