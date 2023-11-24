package uk.co.threebugs.darwinexclient.metatrader.messages

import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.actions.*
import uk.co.threebugs.darwinexclient.metatrader.*

@Service
class MessageService(
    private val actionsService: ActionsService,
    private val messageRepository: MessageRepository,
    private val eventHandler: TradeEventHandler,
    @param:Value("\${sleep-delay}") private val sleepDelay: Int
) {

    fun checkMessages(accountSetupGroupsName: String) {

        if (actionsService.isRunning()) {

            while (true) {
                Helpers.sleep(sleepDelay)

                val newMessages = messageRepository.getNewMessages(accountSetupGroupsName)
                newMessages.forEach {
                    eventHandler.onMessage(it)
                }
            }
        }
    }
}