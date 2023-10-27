package uk.co.threebugs.darwinexclient.actions

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.utils.logger
import java.util.concurrent.atomic.AtomicBoolean

@Service
class ActionsService(@param:Value("\${run-on-startup}") private val runOnStartup: Boolean) {

    private val running = AtomicBoolean(runOnStartup)

    private val startUpComplete = AtomicBoolean(false)

    fun isRunning(): Boolean {
        return startUpComplete.get() && running.get()
    }

    fun setRunning(value: Boolean) {
        running.set(value)
    }

    fun startUpComplete() {
        logger.info("Startup complete")
        startUpComplete.set(true)
    }

}
