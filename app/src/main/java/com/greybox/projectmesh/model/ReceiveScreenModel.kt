package com.greybox.projectmesh.model

import com.greybox.projectmesh.server.AppServer
data class ReceiveScreenModel(
    val incomingTransfers: List<AppServer.IncomingTransferInfo> = emptyList(),
)