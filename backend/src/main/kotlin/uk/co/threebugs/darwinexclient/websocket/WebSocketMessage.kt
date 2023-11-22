package uk.co.threebugs.darwinexclient.websocket

data class WebSocketMessage(
    val id: Int,
    val field: String,
    val value: String
)
