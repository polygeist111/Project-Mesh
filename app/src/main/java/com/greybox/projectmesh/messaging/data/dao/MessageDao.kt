package com.greybox.projectmesh.messaging.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.greybox.projectmesh.messaging.data.entities.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message")
    fun getAllFlow(): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chat = :chat ORDER BY dateReceived ASC")
    fun getChatMessagesFlow(chat: String): Flow<List<Message>>

    @Query("DELETE FROM message")
    fun clearTable()

    @Query("SELECT * FROM message WHERE chat IN (:chatNames) ORDER BY dateReceived ASC")
    fun getChatMessagesFlowMultipleNames(chatNames: List<String>): Flow<List<Message>>

    @Insert
    suspend fun addMessage(m: Message)

    @Delete
    fun delete(m: Message)

    @Delete
    suspend fun deleteAll(messages: List<Message>)

}
