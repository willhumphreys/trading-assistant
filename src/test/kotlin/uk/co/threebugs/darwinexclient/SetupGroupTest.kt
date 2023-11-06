package uk.co.threebugs.darwinexclient

import io.kotest.core.spec.style.*
import uk.co.threebugs.darwinexclient.helpers.SetupRestCallHelper.Companion.getSetupGroup

class SetupGroupTest : FunSpec() {

    init {
        test("Find all setupGroup") {

            getSetupGroup().forEach {
                println(it)
            }

        }
    }
}