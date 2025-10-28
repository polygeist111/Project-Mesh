package com.greybox.projectmesh.bluetooth

import android.util.Log
import com.greybox.projectmesh.messaging.data.entities.Message
import com.ustadmobile.meshrabiya.log.MNetLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.instance
import rawhttp.core.RawHttp
import rawhttp.core.body.StringBody
import java.net.InetAddress
import java.net.URI

/**
 * Client for sending chat messages over Bluetooth.
 * Parallels the Wi-Fi sendChatMessageWithStatus implementation.
 */
class BluetoothMessageClient(
    private val mLogger: MNetLogger,
    private val rawHttp: RawHttp,
    private val json: Json,
    override val di: DI,
    private val localVirtualAddr: InetAddress,
) : org.kodein.di.DIAware {

    // Provided via DI
    private val bluetoothClient: HttpOverBluetoothClient by di.instance()

    /**
     * This function mirrors sendChatMessageWithStatus() from Wi-Fi implementation.
     * 
     * PARALLEL TO WI-FI:
     * - Creates same Message JSON payload
     * - POSTs to same /chat endpoint
     * - Returns boolean success/failure
     */
    suspend fun sendBtChatMessageWithStatus(
        macAddress: String,
        time: Long,
        message: String,
        f: URI?
    ): Boolean {
        return try {
            // Build the same Message object Wi-Fi uses
            val msg = Message(
                id = 0,
                dateReceived = time,
                sender = localVirtualAddr.hostName,   // keep consistent with Wi-Fi path
                chat = macAddress,                    // temporary: use MAC to identify conversation
                content = message,
                file = null                           // keep null until file over BT is implemented
            )

            // Serialize to JSON using kotlinx.serialization (consistent with server)
            val msgJson = json.encodeToString(msg)
            Log.d("BluetoothClient", "Message JSON: $msgJson")

            // Build a Raw HTTP POST /chat request
            // Host header: same pattern from the QR example (MAC with ':' replaced)
            val hostHeader = macAddress.replace(":", "-") + ".bluetooth"
            val request = rawHttp.parseRequest(
                "POST /chat HTTP/1.1\r\n" +
                        "Host: $hostHeader\r\n" +
                        "User-Agent: Meshrabiya\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n"
            ).withBody(StringBody(msgJson))

            // Send it over Bluetooth using the allocation service UUID mask
            val response = bluetoothClient.sendRequest(
                remoteAddress = macAddress,
                uuidMask = BluetoothUuids.ALLOCATION_SERVICE_UUID,
                request = request
            )

            // Check status and close resources
            response.use { btResponse ->
                val code = btResponse.response.statusCode
                val reason = btResponse.response.startLine.reason
                Log.d("BluetoothClient", "BT response: $code $reason")
                code == 200
            }
        } catch (e: SecurityException) {
            Log.e("BluetoothClient", "Security exception sending to $macAddress: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("BluetoothClient", "Failed to send to $macAddress: ${e.message}", e)
            false
        }
    }
}
