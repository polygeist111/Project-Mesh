// Path: app/src/main/java/com/greybox/projectmesh/messaging/network/MessageService.kt
package com.greybox.projectmesh.messaging.network

import com.greybox.projectmesh.messaging.repository.MessageRepository
import com.greybox.projectmesh.messaging.data.entities.Message
import java.net.InetAddress
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class MessageService(
    override val di: DI
) : DIAware {
    private val messageNetworkHandler: MessageNetworkHandler by di.instance()
    private val messageRepository: MessageRepository by di.instance()

    suspend fun sendMessage(address: InetAddress, message: Message) {
        // First save locally
        messageRepository.addMessage(message)

        // Then send over network
        messageNetworkHandler.sendChatMessage(
            address = address,
            time = message.dateReceived,
            message = message.content
        )
    }
}