package com.greybox.projectmesh.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.greybox.projectmesh.messaging.data.dao.MessageDao
import com.greybox.projectmesh.messaging.data.entities.Message

@Database(entities = [Message::class], version = 2)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}