package uk.co.threebugs.darwinexclient.setupgroup


data class SetupGroupFile(
     val path: String,
     val symbol: String,
     val enabled: Boolean,
     val direction: Direction
)
