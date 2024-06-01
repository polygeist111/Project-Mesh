package com.greybox.projectmesh.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
data class UserMessage (
    //@PrimaryKey(autoGenerate = true) val id: Int,
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "dateReceived") val dateReceived: Long

)