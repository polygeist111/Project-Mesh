package com.greybox.projectmesh.messaging.repository

import android.util.Log
import com.greybox.projectmesh.messaging.data.dao.ConversationDao
import com.greybox.projectmesh.messaging.data.entities.Conversation
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.utils.ConversationUtils
import com.greybox.projectmesh.user.UserEntity
import kotlinx.coroutines.flow.Flow
import org.kodein.di.DI
import org.kodein.di.DIAware

class ConversationRepository(
    private val conversationDao: ConversationDao,
    override val di: DI
) : DIAware {

    //get all conversations as a flow
    fun getAllConversations(): Flow<List<Conversation>>{
        return conversationDao.getAllConversationsFlow()
    }

    //get specific convo by id
    suspend fun getConversationById(conversationId: String): Conversation? {
        Log.d("ConversationRepository", "Getting conversation by ID: $conversationId")
        val result = conversationDao.getConversationById(conversationId)
        Log.d("ConversationRepository", "Result for ID $conversationId: ${result != null}")
        return result
    }

    suspend fun getOrCreateConversation(localUuid: String, remoteUser: UserEntity): Conversation {
        //create a unique conversation ID using both UUIDs in order to ensure consistency
        val conversationId = ConversationUtils.createConversationId(localUuid, remoteUser.uuid)

        Log.d("ConversationRepository", "Looking for conversation with ID: $conversationId")
        Log.d("ConversationRepository", "Local UUID: $localUuid, Remote UUID: ${remoteUser.uuid}")

        //try to get an existing conversation
        var conversation = conversationDao.getConversationById(conversationId)

        //if no conversation exists, create a new one
        if(conversation == null){
            Log.d("ConversationRepository", "Conversation not found, creating new one with ${remoteUser.name}")

            conversation = Conversation(
                id = conversationId,
                userUuid = remoteUser.uuid,
                userName = remoteUser.name,
                userAddress = remoteUser.address,
                lastMessage = null,
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = 0,
                isOnline = remoteUser.address != null
            )
            conversationDao.insertConversation(conversation)
            Log.d("ConversationRepository", "Created new conversation with ${remoteUser.name}")
        } else {
            Log.d("ConversationRepository", "Found existing conversation with ${remoteUser.name}")
        }
        return conversation
    }

    //update conversation with the latest message
    suspend fun updateWithMessage(conversationId: String, message: Message) {

        conversationDao.updateLastMessage(
            conversationId = conversationId,
            lastMessage = message.content,
            timestamp = message.dateReceived
        )

        //if this is a received message (not sent by self) increment unread count
        if (message.sender != "Me") {
            conversationDao.incrementUnreadCount(conversationId)
        }

        conversationDao.updateLastMessage(
            conversationId = conversationId,
            lastMessage = message.content,
            timestamp = message.dateReceived
        )

    }

    //mark conversation as read
    suspend fun markAsRead(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
    }

    //update a user's online status
    suspend fun updateUserStatus(userUuid: String, isOnline: Boolean, userAddress: String?) {
        try {
            // Update in database
            conversationDao.updateUserConnectionStatus(
                userUuid = userUuid,
                isOnline = isOnline,
                userAddress = userAddress
            )

            // Log for debugging
            Log.d("ConversationRepository", "Updated user $userUuid connection status: online=$isOnline, address=$userAddress")
        } catch (e: Exception) {
            Log.e("ConversationRepository", "Failed to update user connection status", e)
        }
    }

}
