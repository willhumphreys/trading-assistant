package uk.co.threebugs.darwinexclient

import io.kotest.core.spec.style.*
import uk.co.threebugs.darwinexclient.helpers.SetupRestCallHelper.Companion.getSetupGroups

class SetupGroupsTest : FunSpec() {

    init {
        test("Find all setupGroups") {

            getSetupGroups().forEach {
                println(it)
            }

        }
    }
}