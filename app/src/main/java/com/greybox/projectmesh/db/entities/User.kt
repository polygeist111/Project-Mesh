package com.greybox.projectmesh.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class User(
    //@PrimaryKey(autoGenerate = true) val id: Int,
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "lastSeen") val lastSeen: Long,
    @ColumnInfo(name = "address") val address: Int
)