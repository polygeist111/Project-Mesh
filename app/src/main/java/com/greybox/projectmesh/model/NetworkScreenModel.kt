package com.greybox.projectmesh.model
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState


data class NetworkScreenModel(
    val connectingInProgressSsid: String? = null,
    internal val allNodes: Map<Int, VirtualNode.LastOriginatorMessage> = emptyMap(),
) {
    val nodes: Map<Int, VirtualNode.LastOriginatorMessage>
        get(){
            return allNodes
        }
}