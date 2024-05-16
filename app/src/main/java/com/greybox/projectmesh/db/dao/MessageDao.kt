package com.greybox.projectmesh.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.DeleteTable
import androidx.room.Insert
import androidx.room.Query
import com.greybox.projectmesh.db.entities.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message")
    fun getAllFlow(): Flow<List<Message>>

    @Insert
    suspend fun addMessage(m: Message)


    @Delete
    fun delete(m: Message)

    @Delete
    suspend fun deleteAll(messages: List<Message>)

}
