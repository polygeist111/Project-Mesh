package com.greybox.projectmesh.bluetooth

import android.content.Context
import android.util.Log
import com.greybox.projectmesh.messaging.data.entities.JSONSchema
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.data.dao.MessageDao
import com.greybox.projectmesh.messaging.data.entities.Conversation
import com.greybox.projectmesh.messaging.repository.MessageRepository
import com.greybox.projectmesh.messaging.network.MessageNetworkHandler
import com.greybox.projectmesh.messaging.data.MeshDatabase
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.server.AbstractHttpOverBluetoothServer
import com.ustadmobile.meshrabiya.vnet.bluetooth.VirtualNodeGattServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import rawhttp.core.body.StringBody

class BluetoothServer (
    context: Context,
    private val rawHttp: RawHttp,
    private val logger: MNetLogger,
    private val json: Json,
    private val db: MeshDatabase,
    private val scope: CoroutineScope,
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
        val path = request.uri.path
        val method = request.method
        Log.d("BluetoothServer", "Request $method $path from $remoteDeviceAddress")

        // Only handle POST /chat here
        if (method.equals("POST", ignoreCase = true) && path.startsWith("/chat")) {
            val body = try {
                request.body.map { String(it.asRawBytes()) }.orElse("")
            } catch (e: Exception) {
                Log.e("BluetoothServer", "Failed to read request body", e)
                return textResponse(400, "Bad Request", "Invalid body")
            }

            if (body.isBlank()) {
                Log.e("BluetoothServer", "Empty or missing JSON payload")
                return textResponse(400, "Bad Request", "Empty or missing JSON payload")
            }

            // Validate JSON schema (same as AppServer)
            val schema = JSONSchema()
            if (!schema.schemaValidation(body)) {
                Log.e("BluetoothServer", "Invalid JSON payload by schema")
                return textResponse(400, "Bad Request", """{"error":"Invalid JSON schema"}""")
            }

            // Deserialize -> Message and save to DB, like Wi-Fi
            return try {
                val msg = json.decodeFromString<Message>(body)

                Log.d(
                    "BluetoothServer",
                    "Received chat: '${msg.content}' from ${msg.sender} at ${msg.dateReceived}"
                )

                scope.launch {
                    db.messageDao().addMessage(msg)
                    Log.d("BluetoothServer", "Saved incoming BT message")
                }

                textResponse(200, "OK", "OK")
            } catch (e: Exception) {
                Log.e("BluetoothServer", "Failed to deserialize/save chat", e)
                textResponse(400, "Bad Request", "Invalid JSON")
            }
        }

        // Not found for anything else
        return textResponse(404, "Not Found", "Unknown path")
    }

    private fun textResponse(code: Int, reason: String, body: String): RawHttpResponse<*> {
        return rawHttp.parseResponse(
            "HTTP/1.1 $code $reason\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n"
        ).withBody(StringBody(body))
    }
}