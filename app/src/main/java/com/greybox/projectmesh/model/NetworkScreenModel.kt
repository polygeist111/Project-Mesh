package com.greybox.projectmesh.model

import com.ustadmobile.meshrabiya.vnet.VirtualNode

data class NetworkScreenModel(
    val connectingInProgressSsid: String? = null,
    val allNodes: Map<Int, VirtualNode.LastOriginatorMessage> = emptyMap(),
)