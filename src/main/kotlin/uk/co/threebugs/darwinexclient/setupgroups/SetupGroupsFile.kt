package uk.co.threebugs.darwinexclient.setupgroups

data class SetupGroupsFile (
    val name: String,
    val scriptsDirectory: String,
    val setupGroups: List<SetupGroupFile>
)
