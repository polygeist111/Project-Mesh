package com.greybox.projectmesh.model

import android.net.Uri
import com.ustadmobile.meshrabiya.vnet.VirtualNode

data class SelectDestNodeScreenModel(
    val allNodes: Map<Int, VirtualNode.LastOriginatorMessage> = emptyMap(),
    val uris: List<Uri> = emptyList(),
    val contactingInProgressDevice: String? = null,
)