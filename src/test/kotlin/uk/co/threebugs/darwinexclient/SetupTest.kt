package uk.co.threebugs.darwinexclient

import io.kotest.core.spec.style.*
import uk.co.threebugs.darwinexclient.helpers.SetupRestCallHelper.Companion.getSetups

class SetupTest : FunSpec() {

    init {
        test("Find all setups") {

            getSetups().forEach {
                println(it)
            }

        }
    }
}