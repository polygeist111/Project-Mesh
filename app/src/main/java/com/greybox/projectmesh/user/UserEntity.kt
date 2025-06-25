package com.greybox.projectmesh.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    val address: String? = null, // Default value provided
    val lastSeen: Long? = null
)