package uk.co.threebugs.darwinexclient.helpers

import kotlinx.coroutines.delay
import uk.co.threebugs.darwinexclient.helpers.TimeHelper.Companion.getTime
import uk.co.threebugs.darwinexclient.utils.logger
import kotlin.test.DefaultAsserter.fail

class TimeOutHelper {

    companion object {

        suspend fun waitForCondition(
            timeout: Long,
            interval: Long,
            logMessage: String,
            condition: suspend () -> Boolean
        ) {
            var elapsedTime = 0L
            do {
                logger.info(logMessage)
                logger.info("Client time ${getTime()}")
                if (condition()) {
                    break
                }
                delay(interval)
                elapsedTime += interval
            } while (elapsedTime < timeout)

            if (elapsedTime >= timeout) {
                fail("Condition not met within timeout. $logMessage")
            }
        }
    }
}