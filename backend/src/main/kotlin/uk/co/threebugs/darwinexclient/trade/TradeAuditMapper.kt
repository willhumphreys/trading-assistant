package uk.co.threebugs.darwinexclient.trade

import org.hibernate.envers.*
import org.mapstruct.*

@Mapper(componentModel = "spring")
abstract class TradeAuditMapper {

    @Mappings(
        Mapping(source = "revisionEntity.revisionDate", target = "revisionDate"),
        Mapping(source = "revisionType", target = "revisionType"),
        Mapping(source = "trade.id", target = "id"),
        Mapping(source = "trade.setup.id", target = "setupId"),
        Mapping(source = "trade.account.id", target = "accountId"),
    )
    abstract fun toTradeAuditDTO(
        trade: Trade,
        revisionEntity: DefaultRevisionEntity,
        revisionType: RevisionType
    ): TradeAuditController.TradeAuditDTO

}