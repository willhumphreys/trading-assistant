package uk.co.threebugs.darwinexclient.metatrader

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.co.threebugs.darwinexclient.accountsetupgroups.AccountSetupGroupsDto
import uk.co.threebugs.darwinexclient.actions.ActionsService
import uk.co.threebugs.darwinexclient.metatrader.commands.CommandService
import uk.co.threebugs.darwinexclient.metatrader.data.FileDataService
import uk.co.threebugs.darwinexclient.metatrader.marketdata.MarketDataService
import uk.co.threebugs.darwinexclient.metatrader.messages.MessageService
import uk.co.threebugs.darwinexclient.metatrader.openorders.OpenOrdersService
import uk.co.threebugs.darwinexclient.setupgroup.SetupGroupService
import uk.co.threebugs.darwinexclient.utils.logger
import java.io.IOException
import kotlin.concurrent.thread


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

        try {

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
                    try {
                        openOrdersService.checkOpenOrders(accountSetupGroups)
                    } catch (e: IOException) {
                        logger.warn("Error checking open orders: ${e.message}", e)
                    } catch (e: ConstraintViolationException) {
                        logger.error("Error checking open orders: ${e.message}", e)
                    }
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
        } catch (e: Exception) {
            logger.error("Error starting up client", e)
        }
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
