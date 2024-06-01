package com.greybox.projectmesh.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.greybox.projectmesh.db.dao.MessageDao
import com.greybox.projectmesh.db.dao.UserDao
import com.greybox.projectmesh.db.entities.Message
import com.greybox.projectmesh.db.entities.User

@Database(entities = [Message::class, User::class], version = 2)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
}