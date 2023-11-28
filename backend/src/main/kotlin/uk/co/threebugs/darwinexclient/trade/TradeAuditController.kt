package uk.co.threebugs.darwinexclient.trade

import jakarta.persistence.*
import org.hibernate.envers.*
import org.hibernate.envers.query.*
import org.springframework.web.bind.annotation.*
import uk.co.threebugs.darwinexclient.*
import java.math.*
import java.time.*
import java.util.*

@RestController
@RequestMapping("/audits/trades")
class TradeAuditController(
    private val tradeAuditMapper: TradeAuditMapper
) {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @GetMapping("/{id}")
    fun getTradeRevisions(@PathVariable("id") tradeId: Int): List<TradeAuditDTO> {
        val auditReader = AuditReaderFactory.get(entityManager)
        val revisions = auditReader.createQuery()
            .forRevisionsOfEntity(Trade::class.java, false, true)
            .add(AuditEntity.id().eq(tradeId))
            .resultList


        return revisions.map { revision ->
            val revisionData = revision as Array<*>
            val trade = revisionData[0] as Trade
            val revisionEntity = revisionData[1] as DefaultRevisionEntity
            val revisionType = revisionData[2] as RevisionType



            tradeAuditMapper.toTradeAuditDTO(trade, revisionEntity, revisionType)
        }
    }


    data class TradeAuditDTO(
        val id: Int?,
        val createdDateTime: ZonedDateTime?,
        val lastUpdatedDateTime: ZonedDateTime?,
        val status: Status?,
        val setupId: Int?,
        val accountId: Int?,
        val metatraderId: Int?,
        val placedDateTime: ZonedDateTime?,
        val targetPlaceDateTime: ZonedDateTime?,
        val placedPrice: BigDecimal?,
        val filledDateTime: ZonedDateTime?,
        val filledPrice: BigDecimal?,
        val closedDateTime: ZonedDateTime?,
        val closedPrice: BigDecimal?,
        val closeType: String?,
        val profit: BigDecimal?,
        val message: String?,
        val revisionDate: Date,
        val revisionType: RevisionType
    )
}
