// Path: app/src/main/java/com/greybox/projectmesh/messaging/network/MessageNetworkHandler.kt
package com.greybox.projectmesh.messaging.network

import android.util.Log
import androidx.compose.runtime.remember
import com.google.gson.Gson
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.server.AppServer
import com.greybox.projectmesh.messaging.repository.ConversationRepository
import java.net.InetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kodein.di.DI
import org.kodein.di.DIAware
import com.greybox.projectmesh.user.UserRepository
import kotlinx.coroutines.runBlocking
import android.content.SharedPreferences
import com.greybox.projectmesh.testing.TestDeviceService
import org.kodein.di.instance
import java.net.URI
import com.greybox.projectmesh.messaging.utils.ConversationUtils

class MessageNetworkHandler(
    private val httpClient: OkHttpClient,
    private val localVirtualAddr: InetAddress,
    override val di: DI
) : DIAware {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val conversationRepository: ConversationRepository by di.instance()
    private val settingsPrefs: SharedPreferences by di.instance(tag = "settings")

    //fun sendChatMessage(address: InetAddress, time: Long, message: String) {
    fun sendChatMessage(address: InetAddress, time: Long, message: String, f:URI?/* test this*/) {
        scope.launch {
            try {
                val httpUrl = HttpUrl.Builder()
                    .scheme("http")
                    .host(address.hostAddress)
                    .port(AppServer.DEFAULT_PORT)
                    .addPathSegment("chat")
                    .addQueryParameter("chatMessage", message)
                    .addQueryParameter("time", time.toString())
                    .addQueryParameter("senderIp", localVirtualAddr.hostAddress)
                    .build()

                val request = Request.Builder()
                    .url(httpUrl)
                    .build()
                //Turn this into JSON
                val gs = Gson()
                val msg = Message(
                    id = 0,
                    dateReceived = time,
                    content = message,
                    sender = localVirtualAddr.hostName,
                    chat = address.hostAddress,
                    file = f
                )
                val jsmsg = gs.toJson(msg)
                //send
                httpClient.newCall(request).execute().use { /* response closes automatically */ }
                Log.d("MessageNetworkHandler", "Message sent successfully")
            } catch (e: Exception) {
                Log.e("MessageNetworkHandler", "Failed to send message to ${address.hostAddress}", e)
            }
        }
    }

    companion object {//does this handle json?
    fun handleIncomingMessage(
        chatMessage: String?,
        time: Long,
        senderIp: InetAddress,
        incomingfile: URI?
    ): Message {
        val ipStr = senderIp.hostAddress
        val user = runBlocking {GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)  }// might be null if unknown
        val sender = user?.name ?: "Unknown"

        //determine the correct chat name
        val userUuid = when {
            TestDeviceService.isOnlineTestDevice(senderIp) ->
                "test-device-uuid"
            ipStr == TestDeviceService.TEST_DEVICE_IP_OFFLINE || user?.name == TestDeviceService.TEST_DEVICE_NAME_OFFLINE ->
                "offline-test-device-uuid"
            else -> user?.uuid ?: "unknown-${senderIp.hostAddress}"
        }

        val localUuid = GlobalApp.GlobalUserRepo.prefs.getString("UUID", null) ?: "local-user"
        val chatName = ConversationUtils.createConversationId(localUuid, userUuid)

        Log.d("MessageNetworkHandler", "Creating message with chat name: $chatName, sender: $sender")

        //Create the message
        val message = Message(
            id = 0,
            dateReceived = time,
            content = chatMessage ?: "Error! No message found.",
            sender = sender,
            chat = chatName,
            file = incomingfile//crashes
        )

        //update convo with new message
        if(user != null){
            try {
                //get/create convo
                val conversation = runBlocking {
                    GlobalApp.GlobalUserRepo.conversationRepository.getOrCreateConversation(
                        localUuid = localUuid,
                        remoteUser = user
                    )
                }

                //update convo with the new message
                runBlocking {
                    GlobalApp.GlobalUserRepo.conversationRepository.updateWithMessage(
                        conversationId = conversation.id,
                        message = message
                    )
                }

                Log.d("MessageNetworkHandler", "Updated conversation with new message")
            }catch (e: Exception){
                Log.e("MessageNetworkHandler", "Failed to update conversation", e)
            }
        }
        return message
    }
    }
}