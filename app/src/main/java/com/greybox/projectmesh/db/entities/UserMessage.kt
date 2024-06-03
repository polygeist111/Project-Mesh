package com.greybox.projectmesh.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserMessage (
    //@PrimaryKey(autoGenerate = true) val id: Int,
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "dateReceived") val dateReceived: Long,
    @ColumnInfo(name = "imageUri") val imageUri: String? // Nullable string for image URI
)
