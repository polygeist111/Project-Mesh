// Path: app/src/main/java/com/greybox/projectmesh/messaging/repository/MessageRepository.kt
package com.greybox.projectmesh.messaging.repository

import com.greybox.projectmesh.messaging.data.dao.MessageDao
import com.greybox.projectmesh.messaging.data.entities.Message
import kotlinx.coroutines.flow.Flow
import org.kodein.di.DI
import org.kodein.di.DIAware

// Changed to use Kodein instead of javax.inject
class MessageRepository(
    private val messageDao: MessageDao,
    override val di: DI
) : DIAware {
    // Get all messages for a chat
    fun getChatMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getChatMessagesFlow(chatId)
    }

    // Add a new message
    suspend fun addMessage(message: Message) {
        messageDao.addMessage(message)
    }

    // Get all messages
    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllFlow()
    }

    // Clear all messages
    suspend fun clearMessages() {
        messageDao.clearTable()
    }
}