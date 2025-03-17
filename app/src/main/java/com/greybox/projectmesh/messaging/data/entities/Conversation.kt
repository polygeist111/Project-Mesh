package com.greybox.projectmesh.messaging.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

//Conversation Entity, representing a chat thread with another user

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String, //Composite id of the two users
    @ColumnInfo(name = "user_uuid") val userUuid: String, //other user id
    @ColumnInfo(name = "user_name") val userName: String, //other user name
    @ColumnInfo(name = "user_address") val userAddress: String?, // other user ip address
    @ColumnInfo(name = "last_message") val lastMessage: String?, //last message text
    @ColumnInfo(name = "last_message_time") val lastMessageTime: Long, //Timestamp of last message
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0, //count of unread messages
    @ColumnInfo(name = "is_online") val isOnline: Boolean = false //whether the user is online

)