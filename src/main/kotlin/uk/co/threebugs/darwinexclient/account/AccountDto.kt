package uk.co.threebugs.darwinexclient.account

import java.nio.file.Path


data class AccountDto (
    var id: Int? = null,
    val name: String,
    val metatraderAdvisorPath: Path
)
