package uk.co.threebugs.darwinexclient.metatrader

import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.commands.*
import uk.co.threebugs.darwinexclient.metatrader.data.*
import uk.co.threebugs.darwinexclient.metatrader.marketdata.*
import uk.co.threebugs.darwinexclient.metatrader.messages.*
import uk.co.threebugs.darwinexclient.metatrader.openorders.*
import kotlin.concurrent.*


@Component
class Client(
    private val marketDataService: MarketDataService,
    @param:Value("\${account-setup-groups-name}") private val accountSetupGroupsName: String,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,

    private val actionsService: ActionsService,
    private val messageService: MessageService,
    private val commandService: CommandService,
    private val openOrdersService: OpenOrdersService,
    private val fileDataService: FileDataService,
) {

    init {
        val symbols = arrayOf("EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF")

        val accountSetupGroupsList = fileDataService.loadData(symbols)

        val accountSetupGroups = accountSetupGroupsList.first { it.name == accountSetupGroupsName }

        messageService.loadMessages(accountSetupGroups)

        thread(name = "openOrdersThread") {

            while (true) {
                Helpers.sleep(sleepDelay)
                if (!actionsService.isRunning())
                    continue

                openOrdersService.checkOpenOrders(accountSetupGroups)
            }

        }
        thread(name = "checkMessage") { messageService.checkMessages(accountSetupGroups) }
        thread(name = "checkMarketData") { checkMarketData(accountSetupGroups) }

        commandService.resetCommandIDs(accountSetupGroups)
        openOrdersService.loadOrders(accountSetupGroups)

        // subscribe to tick data:
        commandService.subscribeSymbols(symbols, accountSetupGroups)

        actionsService.startUpComplete()
    }


    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private fun checkMarketData(accountSetupGroupsDto: AccountSetupGroupsDto) {
        while (true) {
            Helpers.sleep(sleepDelay)
            if (!actionsService.isRunning())
                continue

            marketDataService.processUpdates(accountSetupGroupsDto)
        }
    }
}
