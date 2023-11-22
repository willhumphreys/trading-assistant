package uk.co.threebugs.darwinexclient.search

import java.nio.file.Path


data class AccountSearchDto(
    var id: Int? = null,
    var name: String? = null,
    var metatraderAdvisorPath: Path? = null
)
