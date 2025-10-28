
/**
 * Client for sending chat messages over Bluetooth.
 * Parallels the Wi-Fi sendChatMessageWithStatus implementation.
 */
class BluetoothMessageClient(
    private val mLogger: MNetLogger,
    private val rawHttp: RawHttp,
    private val db: MeshDatabase,
    private val json: Json,
    override val di: DI,
    val localVirtualAddr: InetAddress,
    )
    // import client object
    private val bluetoothClient: HttpOverBluetoothClient by di.instance()


    /**
     * This function mirrors sendChatMessageWithStatus() from Wi-Fi implementation.
     * 
     * PARALLEL TO WI-FI:
     * - Creates same Message JSON payload
     * - POSTs to same /chat endpoint
     * - Returns boolean success/failure
     */
    suspend fun sendBluetoothMessageWithStatus(
        macAddress: String,
        time: Long,
        message: String,
        f: URI?
    ): Boolean {
        try {

            // Step 1: Build the Message object (same as wifi)
            val gs = Gson()
            val msg = Message(
                id = 0,
                dateReceived = time,
                sender = localVirtualAddr.hostName,  // this will still be generated even if wifi is unavailable
                chat = macAddress,  // 
                content = message,
                file = null  // theirs is also null
            )
            
            // Step 2: Serialize to JSON (same as wifi)
            val msgJson = gs.toJson(msg)
            Log.d("BluetoothClient", "Message JSON: $msgJson")
            
            // Step 3: Construct raw HTTP POST request
            // same as wifi: Request.Builder().url(httpUrl).post(rbody) -> translated to RawHttp
            // val request = 
            
            // Step 4: Send the request via Bluetooth
            // same as wifi: httpClient.newCall(request).execute().use { response -> ... }
            // val response -> use sendRequest() from BluetoothOverHttpClient
            
            // Step 5: Check response status (same as onDeviceSelected() from QR implementation)
            // reference: https://www.notion.so/grey-box/Bluetooth-QR-Alternative-Branch-247815ddf30b80058b23ff1e483082fd
             response.use {btResponse ->
                when (){

                    // parse response code and return true if ok/ else return false

                }
            }
            
        } catch (e: SecurityException) {
            Log.e("BluetoothClient", "Security exception sending to $macAddress: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e("BluetoothClient", "Failed to send message to $macAddress: ${e.message}", e)
            return false
        }
    }
}

