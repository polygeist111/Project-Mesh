package com.greybox.projectmesh.model

import com.greybox.projectmesh.server.AppServer

data class SendScreenModel(
    val outgoingTransfers: List<AppServer.OutgoingTransferInfo> = emptyList()
)