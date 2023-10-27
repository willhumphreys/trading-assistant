package uk.co.threebugs.darwinexclient.actions

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/actions")
class ActionsController(private val actionsService: ActionsService) {

    @PostMapping("/start")
    fun start() {
        actionsService.setRunning(true)
    }

    @PostMapping("/stop")
    fun stop() {
        actionsService.setRunning(false)
    }
}