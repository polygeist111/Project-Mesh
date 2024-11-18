package com.greybox.projectmesh.model

import com.ustadmobile.meshrabiya.vnet.VirtualNode
import java.net.InetAddress

data class PingScreenModel(
    val deviceName: String? = null,
    val virtualAddress: InetAddress = InetAddress.getByName("192.168.0.1"),
    val allOriginatorMessages: List<VirtualNode.LastOriginatorMessage> = emptyList()
)