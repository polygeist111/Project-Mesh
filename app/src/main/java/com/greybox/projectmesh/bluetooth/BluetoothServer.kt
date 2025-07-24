package com.greybox.projectmesh.bluetooth
import android.content.Context
import android.util.Log
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.server.AbstractHttpOverBluetoothServer
import com.ustadmobile.meshrabiya.vnet.bluetooth.VirtualNodeGattServer
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse


/*
*  A concrete server that serves a connect-link URI over Bluetooth
* */

class BluetoothServer (
    context: Context,
    rawHttp: RawHttp,
    private val logger: MNetLogger,
    private var connectURI: String,
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

    // we initialize the binding with a default string for the URI
    // then update it with this once we are sure that it is generated
    fun updateConnectURI(newURI: String){
        connectURI = newURI
        logger(Log.INFO,"URI has been updated to : $newURI ")
    }

    // this makes sure the default URI is overwritten
    fun hasValidURI(): Boolean {
        return connectURI != "placeholder://will-be-updated"
    }


    /**
     * This function processes the incoming HTTP requests from the client device
     * and returns the correct HTTP response.
     * */
    override fun handleRequest(
        remoteDeviceAddress: String,
        request: RawHttpRequest
    ): RawHttpResponse<*> {
        logger(Log.INFO, "handleRequest from $remoteDeviceAddress : ${request.startLine}")

        // for now, this function only handles the hardcoded URI get request that we defined in
        // onDeviceSelected in HomeScreenViewModel
        return when {
            request.method == "GET" && (request.uri.path == "/api/connect-uri" || request.uri.path == "/") -> {
                logger(Log.INFO, "Sending connect URI to $remoteDeviceAddress")

                rawHttp.parseResponse("HTTP/1.1 200 OK")
                    .withBody(
                        rawhttp.core.body.StringBody(connectURI, "text/plain")
                    )
            }

            else -> {
                logger(Log.WARN, "Unknown request: ${request.method} ${request.uri.path}")

                rawHttp.parseResponse("HTTP/1.1 404 Not Found")
                    .withBody(
                        rawhttp.core.body.StringBody("Not Found", "text/plain")
                    )
            }
        }
    }
}