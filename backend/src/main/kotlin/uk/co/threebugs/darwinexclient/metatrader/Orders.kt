package uk.co.threebugs.darwinexclient.metatrader

import com.fasterxml.jackson.annotation.*

data class Orders(

    @JsonProperty("account_info")
    var accountInfo: AccountInfo,
    var orders: Map<Long, TradeInfo>

)