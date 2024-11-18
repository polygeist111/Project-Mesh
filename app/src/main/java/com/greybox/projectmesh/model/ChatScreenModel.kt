package com.greybox.projectmesh.model

import com.greybox.projectmesh.db.entities.Message
import java.net.InetAddress

data class ChatScreenModel(
    val deviceName: String? = null,
    val virtualAddress: InetAddress = InetAddress.getByName("192.168.0.1"),
    val allChatMessages: List<Message> = emptyList()
)