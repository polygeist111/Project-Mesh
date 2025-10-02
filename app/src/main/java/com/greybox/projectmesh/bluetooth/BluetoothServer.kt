package com.greybox.projectmesh.bluetooth

import android.content.Context
import android.util.Log
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.server.AbstractHttpOverBluetoothServer
import com.ustadmobile.meshrabiya.vnet.bluetooth.VirtualNodeGattServer
import org.kodein.di.Copy
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse

class BluetoothServer (
    context: Context,
    rawHttp: RawHttp,
    private val logger: MNetLogger,
    maxClients: Int = 1
) : AbstractHttpOverBluetoothServer(
    appContext = context,
    rawHttp = rawHttp,
    allocationServiceUuid = BluetoothUuids.ALLOCATION_SERVICE_UUID,
    allocationCharacteristicUuid = BluetoothUuids.ALLOCATION_CHAR_UUID,
    maxClients = maxClients,
    uuidAllocationServerFactory = { appCtx, svcUuid, charUuid, logger, clients, listener ->
        VirtualNodeGattServer(
            appCtx,
            svcUuid,
            charUuid,
            logger,
            clients,
            listener
        )
    }
) {
    override fun handleRequest(
        remoteDeviceAddress: String,
        request: RawHttpRequest,
    ): RawHttpResponse<*> {
        return TODO("Provide the return value")
    }

}