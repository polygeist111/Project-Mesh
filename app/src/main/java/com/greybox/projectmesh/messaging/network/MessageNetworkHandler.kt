// Path: app/src/main/java/com/greybox/projectmesh/messaging/network/MessageNetworkHandler.kt
package com.greybox.projectmesh.messaging.network

import android.util.Log
import androidx.compose.runtime.remember
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.server.AppServer
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

class MessageNetworkHandler(
    private val httpClient: OkHttpClient,
    private val localVirtualAddr: InetAddress,
    override val di: DI
) : DIAware {
    private val scope = CoroutineScope(Dispatchers.IO)
    fun sendChatMessage(address: InetAddress, time: Long, message: String) {
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

                httpClient.newCall(request).execute().use { /* response closes automatically */ }
                Log.d("MessageNetworkHandler", "Message sent successfully")
            } catch (e: Exception) {
                Log.e("MessageNetworkHandler", "Failed to send message to ${address.hostAddress}", e)
            }
        }
    }

    companion object {
        fun handleIncomingMessage(
            chatMessage: String?,
            time: Long,
            senderIp: InetAddress
        ): Message {
            val ipStr = senderIp.hostAddress
            val user = runBlocking {GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)  }// might be null if unknown
            val sender = user?.name ?: "Unknown"
            val chatName = user?.name ?: ipStr
            return Message(
                id = 0,
                dateReceived = time,
                content = chatMessage ?: "Error! No message found.",
                sender = sender,
                chat = chatName
            )
        }
    }
}