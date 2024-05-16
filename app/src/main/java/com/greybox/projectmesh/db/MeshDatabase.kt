package com.greybox.projectmesh.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.greybox.projectmesh.db.dao.MessageDao
import com.greybox.projectmesh.db.entities.Message

@Database(entities = [Message::class], version = 1)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}