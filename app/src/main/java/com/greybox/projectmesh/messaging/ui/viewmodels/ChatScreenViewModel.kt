package com.greybox.projectmesh.messaging.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import com.greybox.projectmesh.testing.TestDeviceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kodein.di.DI
import com.greybox.projectmesh.server.AppServer
import com.greybox.projectmesh.server.AppServer.Companion.DEFAULT_PORT
import com.greybox.projectmesh.server.AppServer.OutgoingTransferInfo
import com.greybox.projectmesh.server.AppServer.Status
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
import com.greybox.projectmesh.messaging.utils.ConversationUtils
import com.greybox.projectmesh.bluetooth.HttpOverBluetoothClient
import com.greybox.projectmesh.bluetooth.BluetoothUuids
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import rawhttp.core.RawHttp
import rawhttp.core.body.StringBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.instance
import java.net.InetAddress
import com.greybox.projectmesh.messaging.repository.ConversationRepository
import com.greybox.projectmesh.user.UserEntity
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.greybox.projectmesh.DeviceStatusManager
import com.greybox.projectmesh.bluetooth.BluetoothMessageClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI

class ChatScreenViewModel(
    di: DI,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val virtualAddress: InetAddress = savedStateHandle.get<InetAddress>("virtualAddress")!!

    // _uiState will be updated whenever there is a change in the UI state
    private val ipStr: String = virtualAddress.hostAddress

    // add btMessageClient
    private val bluetoothMessageClient: BluetoothMessageClient by di.instance()

    //get conversation id
    private val passedConversationId = savedStateHandle.get<String>("conversationId")
    private val sharedPrefs: SharedPreferences by di.instance(tag = "settings")
    private val localUuid = sharedPrefs.getString("UUID", null) ?: "local-user"
    // NEW
    private val isOfflineMode = ipStr == "0.0.0.0" || passedConversationId != null
    //Get User info
    // UPDATED: Get user entity differently based on mode
    private val userEntity = runBlocking {
        if (isOfflineMode && passedConversationId != null) {
            // Extract UUID from conversation ID and load by UUID
            val uuids = passedConversationId.split("-")
            val remoteUuid = uuids.find { it != localUuid }
            remoteUuid?.let {
                GlobalApp.GlobalUserRepo.userRepository.getUser(it)
            }
        } else {
            // Load by IP (existing logic)
            GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)
        }
    }
    // Use the retrieved user name (fallback to "Unknown" if no user is found)
    private val deviceName = userEntity?.name ?: "Unknown"


    // UPDATED: Get userUuid differently based on mode  NEW
    private val userUuid: String = when {
        isOfflineMode && passedConversationId != null -> {
            // Extract from conversation ID
            val uuids = passedConversationId.split("-")
            uuids.find { it != localUuid } ?: "unknown"
        }
        TestDeviceService.isOnlineTestDevice(virtualAddress) -> "test-device-uuid"
        ipStr == TestDeviceService.TEST_DEVICE_IP_OFFLINE -> "offline-test-device-uuid"
        else -> userEntity?.uuid ?: "unknown-${virtualAddress.hostAddress}"
    }

    private val savedConversationId = savedStateHandle.get<String>("conversationId")
    //Log.d("ChatDebug", "GOT CONVERSATION ID FROM SAVED STATE: $savedConversationId")

    private val conversationId = passedConversationId ?:
    ConversationUtils.createConversationId(localUuid, userUuid)

    private val chatName = savedConversationId ?: conversationId
    //Log.d("ChatDebug", "USING CHAT NAME: $chatName (saved: $savedConversationId, generated: $conversationId)")

    // bluetooth only flag
    private val _btOnlyFlag = MutableStateFlow(sharedPrefs.getBoolean("bt_only_mode", false))
    val btOnlyFlag: StateFlow<Boolean> = _btOnlyFlag

    // listener to update the btOnly flag
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "bt_only_mode") {
                _btOnlyFlag.value = sp.getBoolean("bt_only_mode", false)
            }
        }

    override fun onCleared() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }


    private val addressDotNotation = virtualAddress.requireAddressAsInt().addressToDotNotation()

    private val conversationRepository: ConversationRepository by di.instance()


    private val _uiState = MutableStateFlow(
        ChatScreenModel(
            deviceName = deviceName,
            virtualAddress = virtualAddress
        )
    )

    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<ChatScreenModel> = _uiState.asStateFlow()

    // di is used to get the AndroidVirtualNode instance
    private val db: MeshDatabase by di.instance()

    private val appServer: AppServer by di.instance()

    private val rawHttp: RawHttp by di.instance()

    private val json: Json by di.instance()

    private val btClient: HttpOverBluetoothClient by di.instance()

    private val _deviceOnlineStatus = MutableStateFlow(false)
    val deviceOnlineStatus: StateFlow<Boolean> = _deviceOnlineStatus.asStateFlow()

    private val _linkedBtMac = MutableStateFlow<String?>(null)
    val linkedBtMac: StateFlow<String?> = _linkedBtMac.asStateFlow()

    fun setLinkedBluetoothDevice(mac: String?) {
        _linkedBtMac.value = mac
    }

    // launch a coroutine
    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        val savedConversationId = savedStateHandle.get<String>("conversationId")

        // If we have a conversation ID from navigation, use it directly
        val effectiveChatName = if (savedConversationId != null) {
            Log.d("ChatDebug", "USING SAVED CONVERSATION ID: $savedConversationId INSTEAD OF GENERATED: $chatName")
            savedConversationId
        } else {
            chatName
        }

        viewModelScope.launch {
            // Debug logs
            Log.d("ChatDebug", "Will query messages with chatName: $chatName")
            Log.d("ChatDebug", "Using Conversation ID for messages: $conversationId")
            Log.d("ChatDebug", "User UUID: $userUuid")

            //check database content in background
            withContext(Dispatchers.IO) {
                val allMessages = db.messageDao().getAll()
                Log.d("ChatDebug", "All messages in database: ${allMessages.size}")
                for (msg in allMessages) {
                    Log.d(
                        "ChatDebug",
                        "Message: id=${msg.id}, chat=${msg.chat}, content=${msg.content}, sender=${msg.sender}"
                    )
                }
            }

            //determine which flow to collect from
            val isTestDevice =
                (userUuid == "test-device-uuid" || userUuid == "offline-test-device-uuid")

            //load messages synchronously for offline access
            val initialChatName = chatName // Use consistent chat name

            try {
                // Get messages immediately without waiting for Flow
                val initialMessages = withContext(Dispatchers.IO) {
                    // We'll need to add this method to MessageDao in Step 3
                    db.messageDao().getChatMessagesSync(chatName)
                }

                // Update UI immediately with initial messages
                if (initialMessages.isNotEmpty()) {
                    _uiState.update { prev ->
                        prev.copy(allChatMessages = initialMessages)
                    }
                    Log.d("ChatDebfug", "IMMEDIATELY LOADED ${initialMessages.size} MESSAGES FOR OFFLINE ACCESS")
                } else {
                    Log.d("ChatDebug", "NO INITIAL MESSAGES FOUND FOR CHAT: $chatName")
                }

            } catch (e: Exception) {
                Log.e("ChatDebug", "ERROR LOADING INITIAL MESSAGES: ${e.message}", e)
            }

            val messagesFlow = if (isTestDevice) {
                val testDeviceName = when (userUuid) {
                    "test-device-uuid" -> TestDeviceService.TEST_DEVICE_NAME
                    "offline-test-device-uuid" -> TestDeviceService.TEST_DEVICE_NAME_OFFLINE
                    else -> null
                }

                if (testDeviceName != null) {
                    Log.d("ChatDebug", "Using multi-name query with: [$chatName, $testDeviceName]")
                    db.messageDao().getChatMessagesFlowMultipleNames(
                        listOf(chatName, testDeviceName)
                    )
                } else {
                    db.messageDao().getChatMessagesFlow(chatName)
                }
            } else {
                db.messageDao().getChatMessagesFlow(chatName)
            }

            //collect messages from the chosen flow
            messagesFlow.collect { newChatMessages ->
                Log.d("ChatDebug", "Received ${newChatMessages.size} messages")
                _uiState.update { prev ->
                    prev.copy(allChatMessages = newChatMessages)
                }
            }
        }

        viewModelScope.launch {
            // If this is a real device (not placeholder address)
            if (virtualAddress.hostAddress != "0.0.0.0" &&
                virtualAddress.hostAddress != TestDeviceService.TEST_DEVICE_IP_OFFLINE
            ) {
                DeviceStatusManager.deviceStatusMap.collect { statusMap ->
                    val ipAddress = virtualAddress.hostAddress
                    val isOnline = statusMap[ipAddress] ?: false

                    // Only update if status changed
                    if (_deviceOnlineStatus.value != isOnline) {
                        Log.d(
                            "ChatDebug",
                            "Device status changed: $ipAddress is now ${if (isOnline) "online" else "offline"}"
                        )
                        _deviceOnlineStatus.value = isOnline

                        if (isOnline) {
                            Log.d("ChatDebug", "Device came back online - refreshing message history")
                            // Force refresh messages from database
                            withContext(Dispatchers.IO) {
                                val refreshedMessages = db.messageDao().getChatMessagesSync(chatName)
                                _uiState.update { prev ->
                                    prev.copy(
                                        allChatMessages = refreshedMessages,
                                        offlineWarning = null // Clear offline warning
                                    )
                                }
                            }
                        } else {
                            // Update the UI state with offline warning
                            _uiState.update { prev ->
                                prev.copy(
                                    offlineWarning = "Device appears to be offline. Messages will be saved locally."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun markConversationAsRead() {
        try {
            if (userEntity != null) {
                //Create a convo id using both UUIDs
                val conversationId =
                    ConversationUtils.createConversationId(localUuid, userEntity.uuid)

                //Mark this conversation as read
                conversationRepository.markAsRead(conversationId)
                Log.d("ChatScreenViewModel", "Marked conversation as read: $conversationId")
            }
        } catch (e: Exception) {
            Log.e("ChatScreenViewModel", "Error marking conversation as read", e)
        }
    }

    // BLUETOOTH LINKING
    fun linkBluetoothDevice(macAddress: String) {
        viewModelScope.launch {
            try {
                if (userEntity != null) {
                    // Update the user with the MAC address in the database
                    GlobalApp.GlobalUserRepo.userRepository.insertOrUpdateUser(
                        uuid = userUuid,
                        name = userEntity.name,
                        address = userEntity.address,
                        macAddress = macAddress
                    )
                    Log.d("ChatScreenViewModel", "Linked Bluetooth device $macAddress to user $userUuid")

                    // Optional: Verify it was saved correctly
                    val updatedUser = GlobalApp.GlobalUserRepo.userRepository.getUser(userUuid)
                    Log.d("ChatScreenViewModel", "Verified MAC in database: ${updatedUser?.macAddress}")

                } else {
                    Log.w("ChatScreenViewModel", "Cannot link Bluetooth device - userEntity is null")
                }
            } catch (e: Exception) {
                Log.e("ChatScreenViewModel", "Error linking Bluetooth device", e)
            }
        }
    }

    /*
 * This is the Bluetooth equivalent of sendChatMessage() in ChatScreenViewModel
 *
 * This function:
 * 1. Checks if a device is linked
 * 2. Saves message to local database
 * 3. Updates local conversation
 * 4. Sends message via Bluetooth
 */
    fun sendBluetoothChatMessage(message: String, file: URI?) {
        val sendTime = System.currentTimeMillis()
        // TO DO: Implement a mechanism to make sure there is a valid BT device
        //         before launching a coroutine

        viewModelScope.launch {
            // current user is the most recent version of the remote device
            val currentUser = GlobalApp.GlobalUserRepo.userRepository.getUser(userUuid)

            // this will make sure a device is linked
            if (currentUser?.macAddress == null) {
                Log.w("ChatScreenViewModel", "No linked Bluetooth device selected")
                _uiState.update { prev ->
                    prev.copy(offlineWarning = "No Bluetooth device selected. Pick a device first.")
                }
                return@launch
            }

            // added logs for debugging
            val linkedMacAddress = currentUser.macAddress
            Log.d("ChatScreenViewModel", "Sending Bluetooth message to MAC: $linkedMacAddress")

            // Step 2: Create message entity (same as Wi-Fi)
            // Note: keeping file=null for now to mirror skeleton; wire file transfers later.
            val messageEntity = Message(
                id = 0,
                dateReceived = sendTime,
                content = message,
                sender = "Me",
                chat = chatName,
                file = file
            )

            // Step 3: Save to local database (same as Wi-Fi)
            db.messageDao().addMessage(messageEntity)

            // Step 4: Update conversation with the message
            if (currentUser != null) {  // Use currentUser instead of userEntity
                try {
                    val remoteUser = UserEntity(
                        uuid = userUuid,
                        name = currentUser.name,
                        address = currentUser.address,
                        macAddress = currentUser.macAddress
                    )

                    val conversation = conversationRepository.getOrCreateConversation(
                        localUuid = localUuid,
                        remoteUser = remoteUser
                    )

                    conversationRepository.updateWithMessage(
                        conversationId = conversation.id,
                        message = messageEntity
                    )

                    Log.d("ChatScreenViewModel", "Updated conversation with sent Bluetooth message")
                } catch (e: Exception) {
                    Log.e("ChatScreenViewModel", "Failed to update conversation", e)
                }
            }

            // Step 5: Send via Bluetooth
            // changed to mirror sendChatMessage and let the btClient handle the HTTP/response
            try {
                val success = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(5000) {
                        bluetoothMessageClient.sendBtChatMessageWithStatus(
                            macAddress = linkedMacAddress,
                            time = sendTime,
                            message = message,
                            f = file
                        )
                    } ?: false
                }

                if (success) {
                    Log.d("ChatScreenViewModel", "Bluetooth message sent successfully to $linkedMacAddress")
                } else {
                    Log.e(
                        "ChatScreenViewModel",
                        "Failed to send Bluetooth message to $linkedMacAddress"
                    )
                    _uiState.update { prev ->
                        prev.copy(offlineWarning = "Bluetooth message delivery failed.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreenViewModel", "Error sending Bluetooth message: ${e.message}", e)
                _uiState.update { prev ->
                    prev.copy(offlineWarning = "Error sending Bluetooth message: ${e.message}")
                }
            }
        }
    }

    fun sendChatMessage(
        virtualAddress: InetAddress,
        message: String,
        file: URI?
    ) {//add file field here
        val ipAddress = virtualAddress.hostAddress
        val sendTime: Long = System.currentTimeMillis()

        //check if device is online first
        val isOnline = DeviceStatusManager.isDeviceOnline(ipAddress)

        //use same conversationid as chat name
        val messageEntity = Message(0, sendTime, message, "Me", chatName, file)

        Log.d("ChatDebug", "Sending message to chat: $chatName")
        viewModelScope.launch {
            //save to local database
            db.messageDao().addMessage(messageEntity)

            //update convo with the new message
            if (userEntity != null) {
                try {
                    //get or create conversation
                    val remoteUser = UserEntity(
                        uuid = userUuid,
                        name = userEntity.name,
                        address = userEntity.address
                    )

                    val conversation = conversationRepository.getOrCreateConversation(
                        localUuid = localUuid,
                        remoteUser = remoteUser
                    )

                    //update conversation with the message
                    conversationRepository.updateWithMessage(
                        conversationId = conversation.id,
                        message = messageEntity
                    )

                    Log.d("ChatScreenViewModel", "Updated conversation with sent message")
                } catch (e: Exception) {
                    Log.e(
                        "ChatScreenViewModel",
                        "Failed to update conversation with sent message",
                        e
                    )
                }
            }

            if (isOnline) {
                try {
                    // Use withContext to ensure network operations run on IO thread
                    val delivered = withContext(Dispatchers.IO) {
                        // Try with a timeout to prevent blocking
                        withTimeoutOrNull(5000) {
                            appServer.sendChatMessageWithStatus(virtualAddress, sendTime, message, file)
                        } ?: false
                    }

                    // Update UI based on delivery status
                    if (!delivered) {
                        Log.d("ChatDebug", "Message delivery failed")
                        _uiState.update { prev ->
                            prev.copy(offlineWarning = "Message delivery failed. Device may be offline.")
                        }
                        // Force device status verification
                        DeviceStatusManager.verifyDeviceStatus(ipAddress)
                    } else {
                        Log.d("ChatDebug", "Message delivered successfully")
                    }
                } catch (e: Exception) {
                    Log.e("ChatScreenViewModel", "Error sending message: ${e.message}", e)
                    _uiState.update { prev ->
                        prev.copy(offlineWarning = "Error sending message: ${e.message}")
                    }
                }
            } else {
                Log.d("ChatScreenViewModel", "Device $ipAddress appears to be offline, message saved locally only")
                _uiState.update { prev ->
                    prev.copy(offlineWarning = "Device appears to be offline. Message saved locally only.")
                }
            }
        }
    }

    //handles outgoing file transfer to fix unresolved reference error crash
    fun addOutgoingTransfer(fileUri: Uri, toAddress: InetAddress): OutgoingTransferInfo {
        return appServer.addOutgoingTransfer(fileUri, toAddress)
    }

/*
 * Bluetooth equivalent of sendChatMessage() in ChatScreenViewModel
 *
 * This function:
 * 1) Checks if a device is linked
 * 2) Saves message to local database
 * 3) Updates local conversation
 * 4) Sends message via Bluetooth (parallel to AppServer.sendChatMessageWithStatus)
 * 5) Logs success/failure
 */
fun sendBluetoothChatMessage(message: String, file: URI?) {
    val sendTime = System.currentTimeMillis()

    // Step 1: Check if a device is linked
    val macAddress = _linkedBtMac.value
    if (macAddress.isNullOrBlank()) {
        Log.e("ChatScreenViewModel", "No linked Bluetooth device selected")
        _uiState.update { prev ->
            prev.copy(offlineWarning = "No Bluetooth device selected. Pick a device first.")
        }
        return
    }

    // Step 2: Create message entity (same as Wi-Fi)
    // Note: keeping file=null for now to mirror skeleton; wire file transfers later.
    val messageEntity = Message(
        id = 0,
        dateReceived = sendTime,
        content = message,
        sender = "Me",
        chat = chatName,
        file = null
    )

    viewModelScope.launch {
        // Step 3: Save to local database (same as Wi-Fi)
        db.messageDao().addMessage(messageEntity)

        // Step 4: Update conversation with the message (same as Wi-Fi)
        if (userEntity != null) {
            try {
                // (Skeleton-style TODO) extend UserEntity to include macAddress, then:
                /*
                val remoteUser = UserEntity(
                    uuid = userUuid,
                    name = userEntity.name,
                    address = userEntity.address,
                    macAddress = macAddress
                )
                val conversation = conversationRepository.getOrCreateConversation(
                    localUuid = localUuid,
                    remoteUser = remoteUser
                )
                conversationRepository.updateWithMessage(
                    conversationId = conversation.id,
                    message = messageEntity
                )
                */
                Log.d("ChatScreenViewModel", "Updated conversation with sent Bluetooth message")
            } catch (e: Exception) {
                Log.e("ChatScreenViewModel", "Failed to update conversation", e)
            }
        }

        // Step 5: Send via Bluetooth (parallel to Wi-Fi's HTTP POST /chat)
        // Build network copy for the peer: use our virtual IP as 'sender'
        val networkMsg = messageEntity.copy(sender = addressDotNotation)
        val msgJson = json.encodeToString(networkMsg)

        // Raw HTTP request body and headers (same shape as Wi-Fi; different transport)
        val hostHeader = macAddress.replace(":", "-") + ".bluetooth"
        val request = rawHttp
            .parseRequest(
                "POST /chat HTTP/1.1\r\n" +
                        "Host: $hostHeader\r\n" +
                        "User-Agent: Meshrabiya\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n"
            )
            .withBody(StringBody(msgJson))

        val success = withContext(Dispatchers.IO) {
            withTimeoutOrNull(5000) {
                btClient.sendRequest(
                    remoteAddress = macAddress,
                    uuidMask = BluetoothUuids.ALLOCATION_SERVICE_UUID,
                    request = request
                ).use { resp ->
                    val code = resp.response.statusCode
                    Log.d("ChatScreenViewModel",
                        "BT HTTP response: $code ${resp.response.startLine.reason}")
                    code == 200
                }
            } ?: false
        }

        if (success) {
            Log.d("ChatScreenViewModel", "Bluetooth message sent successfully to $macAddress")
        } else {
            Log.e("ChatScreenViewModel", "Failed to send Bluetooth message to $macAddress")
            _uiState.update { prev ->
                prev.copy(offlineWarning = "Bluetooth delivery failed. Device may be offline.")
            }
        }
    }
}
