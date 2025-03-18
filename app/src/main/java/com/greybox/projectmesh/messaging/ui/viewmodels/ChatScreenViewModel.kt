package com.greybox.projectmesh.messaging.ui.viewmodels

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
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
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

class ChatScreenViewModel(
    di: DI,
    virtualAddress: InetAddress
): ViewModel() {
    // _uiState will be updated whenever there is a change in the UI state
    private val ipStr: String = virtualAddress.hostAddress
    private val userEntity = runBlocking {
        GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)
    }

    // Use the retrieved user name (fallback to "Unknown" if no user is found)
    private val deviceName = userEntity?.name ?: "Unknown"

    private val sharedPrefs: SharedPreferences by di.instance(tag = "settings")
    private val localUuid = sharedPrefs.getString("UUID", null) ?: "local-user"

    private val userUuid: String = when {
        //if its the online test device
        TestDeviceService.isOnlineTestDevice(virtualAddress) ->
            "test-device-uuid"

        //if it's the offline test device
        ipStr == TestDeviceService.TEST_DEVICE_IP_OFFLINE ||
                userEntity?.name == TestDeviceService.TEST_DEVICE_NAME_OFFLINE ->
            "offline-test-device-uuid"

        //for any other user, use their name if available, otherwise use IP
        else -> userEntity?.uuid ?: "unknown-${virtualAddress.hostAddress}"
    }

    private val conversationId = createConversationId(localUuid, userUuid)
    private val chatName = conversationId //use conversation id as chat name

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

    private fun createConversationId(uuid1: String, uuid2: String): String {
        //special cases for test devices
        if (uuid2 == "test-device-uuid") {
            return "local-user-test-device-uuid"
        }
        if (uuid2 == "offline-test-device-uuid") {
            return "local-user-offline-test-device-uuid"
        }
        return listOf(uuid1, uuid2).sorted().joinToString("-")
    }

    // launch a coroutine
    init {
        viewModelScope.launch {
            // Debug logs
            Log.d("ChatDebug", "Will query messages with chatName: $chatName")
            Log.d("ChatDebug", "Conversation ID: $conversationId")
            Log.d("ChatDebug", "User UUID: $userUuid")

            //check database content in background
            withContext(Dispatchers.IO) {
                val allMessages = db.messageDao().getAll()
                Log.d("ChatDebug", "All messages in database: ${allMessages.size}")
                for (msg in allMessages) {
                    Log.d("ChatDebug", "Message: id=${msg.id}, chat=${msg.chat}, content=${msg.content}, sender=${msg.sender}")
                }
            }

            //determine which flow to collect from
            val isTestDevice = (userUuid == "test-device-uuid" || userUuid == "offline-test-device-uuid")
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
    }

    private suspend fun markConversationAsRead() {
        try {
            if(userEntity != null){
                //Create a convo id using both UUIDs
                val conversationId = createConversationId(localUuid, userEntity.uuid)

                //Mark this conversation as read
                conversationRepository.markAsRead(conversationId)
                Log.d("ChatScreenViewModel", "Marked conversation as read: $conversationId")
            }
        }catch (e: Exception){
            Log.e("ChatScreenViewModel", "Error marking conversation as read", e)
        }
    }


    fun sendChatMessage(virtualAddress: InetAddress, message: String) {
        val sendTime: Long = System.currentTimeMillis()

        //use same conversationid as chat name
        val messageEntity = Message(0, sendTime, message, "Me", chatName)

        Log.d("ChatDebug", "Sending message to chat: $chatName")


        viewModelScope.launch {
            //save to local database
            db.messageDao().addMessage(messageEntity)

            //update convo with the new message
            if (userEntity != null){
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
                } catch (e:Exception){
                    Log.e("ChatScreenViewModel", "Failed to update conversation with sent message", e)
                }
            }
        }
        appServer.sendChatMessage(virtualAddress, sendTime, message)
    }
}