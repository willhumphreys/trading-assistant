package uk.co.threebugs.darwinexclient.utils

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class StartupExceptionLogger : ApplicationListener<ApplicationFailedEvent> {
    private val logger = LoggerFactory.getLogger(StartupExceptionLogger::class.java)

    override fun onApplicationEvent(event: ApplicationFailedEvent) {
        logger.error("Application startup failed:", event.exception)
    }
}