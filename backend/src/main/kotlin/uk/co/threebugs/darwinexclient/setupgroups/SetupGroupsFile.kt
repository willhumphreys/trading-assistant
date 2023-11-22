package uk.co.threebugs.darwinexclient.setupgroups

import uk.co.threebugs.darwinexclient.setupgroup.*

data class SetupGroupsFile(
    val name: String,
    val scriptsDirectory: String,
    val setupGroups: List<SetupGroupFile>
)
