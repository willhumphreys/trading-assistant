package uk.co.threebugs.mochiwhattotrade3.account

import java.nio.file.Path


data class AccountDto (
    var id: Int? = null,
    var name: String? = null,
    var metatraderAdvisorPath: Path? = null
)
