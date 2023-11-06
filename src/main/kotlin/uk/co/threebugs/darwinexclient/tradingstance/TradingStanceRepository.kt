package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.*
import org.springframework.stereotype.*

@Repository
interface TradingStanceRepository : JpaRepository<TradingStance, Int>, QueryByExampleExecutor<TradingStance> {
    fun findByAccount_Name(name: String, sort: Sort): List<TradingStance>
}