package com.greybox.projectmesh.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.Date

@Entity
@Serializable
data class User(
    //@PrimaryKey(autoGenerate = true) val id: Int,
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "lastSeen") var lastSeen: Long,
    @ColumnInfo(name = "address") var address: Int
)