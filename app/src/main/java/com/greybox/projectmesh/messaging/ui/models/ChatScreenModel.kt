package com.greybox.projectmesh.messaging.ui.models

import com.greybox.projectmesh.messaging.data.entities.Message
import java.net.InetAddress

data class ChatScreenModel(
    val deviceName: String? = null,
    val virtualAddress: InetAddress = InetAddress.getByName("192.168.0.1"),
    val allChatMessages: List<Message> = emptyList()
)