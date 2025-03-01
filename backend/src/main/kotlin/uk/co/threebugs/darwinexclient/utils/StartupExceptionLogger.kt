package uk.co.threebugs.darwinexclient.utils

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class StartupExceptionLogger : ApplicationListener<ApplicationFailedEvent> {
    private val logger = LoggerFactory.getLogger(StartupExceptionLogger::class.java)

    override fun onApplicationEvent(event: ApplicationFailedEvent) {
        val exception = event.exception

        // Log the top-level exception with its full stack trace.
        logger.error("Application startup failed: ${exception.message}", exception)

        // Log the full cause chain.
        logCauseChain(exception)
    }

    private fun logCauseChain(throwable: Throwable?) {
        var cause = throwable?.cause
        while (cause != null) {
            logger.error("Caused by: ${cause.javaClass.simpleName}: ${cause.message}", cause)
            cause = cause.cause
        }
    }
}