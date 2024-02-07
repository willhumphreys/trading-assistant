package uk.co.threebugs.darwinexclient.metatrader

import io.micrometer.core.instrument.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.commands.*
import uk.co.threebugs.darwinexclient.metatrader.data.*
import uk.co.threebugs.darwinexclient.metatrader.marketdata.*
import uk.co.threebugs.darwinexclient.metatrader.messages.*
import uk.co.threebugs.darwinexclient.metatrader.openorders.*
import uk.co.threebugs.darwinexclient.setupgroup.*
import kotlin.concurrent.*


@Component
class Client(
    private val marketDataService: MarketDataService,
    @param:Value("\${account-setup-groups-name}") private val accountSetupGroupsName: String,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,
    @param:Value("\${setup-limit}") private val setupLimit: Int,

    private val actionsService: ActionsService,
    private val messageService: MessageService,
    private val setupGroupService: SetupGroupService,
    commandService: CommandService,
    private val openOrdersService: OpenOrdersService,
    fileDataService: FileDataService,
    meterRegistry: MeterRegistry
) {

    init {
        val accountSetupGroups = getAccountSetupGroups(fileDataService)

        messageService.loadMessages(accountSetupGroups)


        val openOrdersThreadCounter = Counter.builder("api_order_loop_counter")
            .tag("title", "Order Loop Counter")
            .description("Loops of the open orders thread")
            .register(meterRegistry)


        openOrdersService.loadOrders(accountSetupGroups)

        thread(name = "openOrdersThread") {

            while (true) {
                Helpers.sleep(sleepDelay)
                if (!actionsService.isRunning())
                    continue

                openOrdersThreadCounter.increment()
                openOrdersService.checkOpenOrders(accountSetupGroups)
            }

        }
        thread(name = "checkMessage") { messageService.checkMessages(accountSetupGroups) }
        thread(name = "checkMarketData") { checkMarketData(accountSetupGroups) }

        commandService.resetCommandIDs(accountSetupGroups)

        val uniqueSymbols =
            setupGroupService.findUniqueSymbolsBySetupGroups(accountSetupGroups.setupGroups)

        // subscribe to tick data:
        commandService.subscribeSymbols(uniqueSymbols, accountSetupGroups)

        actionsService.startUpComplete()
    }

    private fun getAccountSetupGroups(
        fileDataService: FileDataService
    ): AccountSetupGroupsDto {

        try {
            val accountSetupGroupsList = fileDataService.loadData(setupLimit)

            return accountSetupGroupsList.first { it.name == accountSetupGroupsName }
        } catch (e: NoSuchElementException) {
            throw RuntimeException("Account setup group $accountSetupGroupsName not found")
        }
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
