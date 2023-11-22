package uk.co.threebugs.darwinexclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DarwinexClientApplication

fun main(args: Array<String>) {
    runApplication<DarwinexClientApplication>(*args)
}
