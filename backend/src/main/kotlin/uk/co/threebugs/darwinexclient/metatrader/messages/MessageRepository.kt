package uk.co.threebugs.darwinexclient.metatrader.messages

import org.json.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.accountsetupgroups.*
import uk.co.threebugs.darwinexclient.metatrader.*
import java.util.*

@Repository
class MessageRepository {

    private var lastMessagesStr: String? = ""
    private var lastMessagesMillis: Long = 0


    /*Loads stored messages from file (in case of a restart).
 */
    fun loadMessages(accountSetupGroups: AccountSetupGroupsDto) {

        val dwxPath = accountSetupGroups.account.metatraderAdvisorPath.resolve("DWX")

        val text = Helpers.tryReadFile(
            dwxPath.resolve("DWX_Messages_Stored.json")
                ?: throw NoSuchElementException("Key 'pathMessagesStored' not found")
        )
        if (text.isEmpty()) return
        val data: JSONObject
        data = try {
            JSONObject(text)
        } catch (e: Exception) {
            return
        }
        lastMessagesStr = text

        // here we don't have to sort because we just need the latest millis value.
        for (millisStr in data.keySet()) {
            if (data.has(millisStr)) {
                val millis = millisStr.toLong()
                if (millis > lastMessagesMillis) lastMessagesMillis = millis
            }
        }
    }

    fun getNewMessages(accountSetupGroupsDto: AccountSetupGroupsDto): MutableList<JSONObject> {

        val dwxPath = accountSetupGroupsDto.account.metatraderAdvisorPath.resolve("DWX")

        val newMessages = mutableListOf<JSONObject>()

        val messagesPath =
            dwxPath.resolve("DWX_Messages.json") ?: throw NoSuchElementException("Key 'pathMessages' not found")
        val text = Helpers.tryReadFile(messagesPath)
        if (text.isEmpty() || text == lastMessagesStr) return newMessages
        lastMessagesStr = text
        val data: JSONObject = try {
            JSONObject(text)
        } catch (e: Exception) {
            return newMessages
        }

        // the objects are not ordered. because of (millis > lastMessagesMillis) it would miss messages if we just looped through them directly.
        val millisList = ArrayList<String>()
        for (millisStr in data.keySet()) {
            if (data[millisStr] != null) {
                millisList.add(millisStr)
            }
        }
        Collections.sort(millisList)
        for (millisStr in millisList) {
            if (data[millisStr] != null) {
                val millis = millisStr.toLong()
                if (millis > lastMessagesMillis) {
                    lastMessagesMillis = millis
                    newMessages.add(data[millisStr] as JSONObject)
                    //  eventHandler.onMessage(this, data[millisStr] as JSONObject)
                }
            }
        }
        Helpers.tryWriteToFile(messagesPath, data.toString())


        return newMessages
    }


}