package com.greybox.projectmesh.messaging.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greybox.projectmesh.messaging.data.entities.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY last_message_time DESC")
    fun getAllConversationsFlow(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): Conversation?

    @Query("SELECT * FROM conversations WHERE user_uuid = :userUuid LIMIT 1")
    suspend fun getConversationByUserUuid(userUuid: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Query("UPDATE conversations SET is_online = :isOnline, user_address = :userAddress WHERE user_uuid = :userUuid")
    suspend fun updateUserConnectionStatus(userUuid: String, isOnline: Boolean, userAddress: String?)

    @Query("UPDATE conversations SET last_message = :lastMessage, last_message_time = :timestamp WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE conversations SET unread_count = unread_count + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)

    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)
}