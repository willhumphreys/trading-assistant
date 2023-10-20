package uk.co.threebugs.darwinexclient.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

val <T : Any> T.logger: Logger
    get() = LogManager.getLogger(this.javaClass)
