package uk.co.threebugs.mochiwhattotrade3.setupgroups

data class SetupGroupsFile (
    val name: String,
    val scriptsDirectory: String,
    val setupGroups: List<SetupGroupFile>
)
