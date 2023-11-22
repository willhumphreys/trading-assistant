package uk.co.threebugs.darwinexclient.actions

import org.springframework.web.bind.annotation.*
import uk.co.threebugs.darwinexclient.tradingstance.*

@RestController
@RequestMapping("/actions")
class ActionsController(
    private val actionsService: ActionsService,
    private val tradingStanceService: TradingStanceService
) {

    @PostMapping("/start")
    fun start() {
        actionsService.setRunning(true)
    }

    @PostMapping("/stop")
    fun stop() {
        actionsService.setRunning(false)
    }

    @PostMapping("/update-trading-stance/{id}")
    fun updateTradingStance(
        @PathVariable id: Int,
        @RequestBody tradingStanceDto: UpdateTradingStanceDto
    ): TradingStanceDto {
        return tradingStanceService.updateTradingStance(id, tradingStanceDto)
    }

}