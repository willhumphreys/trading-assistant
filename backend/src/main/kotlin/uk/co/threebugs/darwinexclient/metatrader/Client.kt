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
import kotlin.concurrent.*


@Component
class Client(
    private val marketDataService: MarketDataService,
    @param:Value("\${account-setup-groups-name}") private val accountSetupGroupsName: String,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int,
    @param:Value("\${setup-limit}") private val setupLimit: Int,

    private val actionsService: ActionsService,
    private val messageService: MessageService,
    commandService: CommandService,
    private val openOrdersService: OpenOrdersService,
    fileDataService: FileDataService,
    private val meterRegistry: MeterRegistry


) {

    init {
        val symbols = listOf("EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF")

        val accountSetupGroups = getAccountSetupGroups(fileDataService, symbols)

        messageService.loadMessages(accountSetupGroups)


        val openOrdersThreadCounter = Counter.builder("api_books_get")
            .tag("title", "Orders")
            .description("Loops of the open orders thread")
            .register(meterRegistry)

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
        openOrdersService.loadOrders(accountSetupGroups)

        // subscribe to tick data:
        commandService.subscribeSymbols(symbols, accountSetupGroups)

        actionsService.startUpComplete()
    }

    private fun getAccountSetupGroups(
        fileDataService: FileDataService,
        symbols: List<String>
    ): AccountSetupGroupsDto {

        try {
            val accountSetupGroupsList = fileDataService.loadData(symbols, setupLimit)

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
