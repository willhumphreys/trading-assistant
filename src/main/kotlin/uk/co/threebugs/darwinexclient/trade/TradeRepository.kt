package uk.co.threebugs.darwinexclient.trade

import jakarta.transaction.*
import org.springframework.data.domain.*
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import uk.co.threebugs.darwinexclient.setup.*
import java.time.*

@Repository
interface TradeRepository : JpaRepository<Trade, Int>, QueryByExampleExecutor<Trade> {
    // fun findByStatusAndSetup_SymbolAndAccount(status: Status, symbol: String, account: Account): List<Trade>

    @Query(
        value = "SELECT t.* FROM trade t " +
                "JOIN setup s ON t.setup_id = s.id " +
                "JOIN setup_group sg ON s.setup_group_id = sg.id " +
                "JOIN setup_groups sgs ON sg.setup_groups_id = sgs.id " +
                "JOIN account_setup_groups asg ON asg.setup_groups_id = sgs.id " +
                "WHERE asg.id = :accountSetupGroupsId and s.symbol = :symbol and t.status = :status", nativeQuery = true
    )
    fun findByAccountSetupGroupsSymbolAndStatus(
        accountSetupGroupsId: Int,
        symbol: String,
        status: String,
    ): List<Trade>


    @Query(
        value = "SELECT t.* FROM trade t " +
                "JOIN setup s ON t.setup_id = s.id " +
                "JOIN setup_group sg ON s.setup_group_id = sg.id " +
                "JOIN setup_groups sgs ON sg.setup_groups_id = sgs.id " +
                "JOIN account_setup_groups asg ON asg.setup_groups_id = sgs.id " +
                "WHERE asg.id = :accountSetupGroupsId and s.symbol = :symbol", nativeQuery = true
    )
    fun findByAccountSetupGroupsAndSymbol(
        accountSetupGroupsId: Int,
        symbol: String
    ): List<Trade>


    fun findBySetupAndTargetPlaceDateTimeAndAccount(
        setup: Setup,
        placedDateTime: ZonedDateTime,
        account: Account
    ): Trade?

    fun findByAccount_Name(name: String, sort: Sort): List<Trade>

    @Transactional
    @Modifying
    @Query(
        value = """
    DELETE FROM trade
    WHERE id IN (
        SELECT * FROM (
            SELECT t.id FROM trade t
            JOIN setup s ON t.setup_id = s.id
            JOIN setup_group sg ON s.setup_group_id = sg.id
            JOIN setup_groups sgs ON sg.setup_groups_id = sgs.id
            WHERE sgs.name = :name
        ) AS temp
    )
""", nativeQuery = true
    )
    fun deleteBySetupGroupsName(@Param("name") name: String): Int

    @Transactional
    @Modifying
    @Query(
        value = """
    DELETE FROM trade
    WHERE id IN (
        SELECT * FROM (
            SELECT t.id FROM trade t
            JOIN account a ON t.account_id = a.id
            WHERE a.name = :name
        ) AS temp
    )
""", nativeQuery = true
    )
    fun deleteByAccountName(@Param("name") name: String): Int

    @Query(
        "SELECT t " +
                "FROM Trade t " +
                "JOIN t.setup s " +
                "JOIN s.setupGroup sg " +
                "JOIN sg.setupGroups sgs " +
                "WHERE sgs.name = :name"
    )
    fun findBySetupGroupsName(name: String): List<Trade>

//    @Query(
//        value = "SELECT t.* FROM trade t " +
//                "JOIN account a ON t.account_id = a.id " +
//                "JOIN account_setup_groups asg ON asg.account_id = a.id " +
//                "WHERE asg.name = :name", nativeQuery = true
//    )
//    fun findTradesByAccountSetupGroupsName(name: String)

    @Query(
        value = """
        SELECT t.* FROM trade t
        JOIN setup s ON t.setup_id = s.id
        JOIN setup_group sg ON s.setup_group_id = sg.id
        JOIN setup_groups sgs ON sg.setup_groups_id = sgs.id
        JOIN account_setup_groups asg ON asg.setup_groups_id = sgs.id
        WHERE asg.id = :accountSetupGroupsId and s.id = :setupId and t.target_place_date_time = :targetPlaceDateTime
    """, nativeQuery = true
    )
    fun findBySetupAndTargetPlaceDateTimeAndAccountSetupGroups(
        @Param("accountSetupGroupsId") accountSetupGroupsId: Int,
        @Param("setupId") setupId: Int,
        @Param("targetPlaceDateTime") targetPlaceDateTime: String
    ): Trade?

}
