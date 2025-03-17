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

    private val chatName: String = when {
        //if its the online test device
        TestDeviceService.isOnlineTestDevice(virtualAddress) ->
            TestDeviceService.TEST_DEVICE_NAME

        // If it's the offline test device
        ipStr == TestDeviceService.TEST_DEVICE_IP_OFFLINE ||
                userEntity?.name == TestDeviceService.TEST_DEVICE_NAME_OFFLINE ->
            TestDeviceService.TEST_DEVICE_NAME_OFFLINE

        // For any other user, use their name if available, otherwise use IP
        else -> userEntity?.name ?: ipStr
    }

    private val addressDotNotation = virtualAddress.requireAddressAsInt().addressToDotNotation()

    private val conversationRepository: ConversationRepository by di.instance()
    private val sharedPrefs: SharedPreferences by di.instance(tag = "settings")
    private val localUuid = sharedPrefs.getString("UUID", null) ?: "local-user"

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


    // launch a coroutine
    init {
        viewModelScope.launch {
            // Log the chat name we're using
            Log.d("ChatDebug", "Initializing with chatName: $chatName")

            // IMPORTANT: Move database query to a background thread
            withContext(Dispatchers.IO) {
                // Log all messages in the database (safely on a background thread)
                val allMessages = db.messageDao().getAll()
                Log.d("ChatDebug", "All messages in database: ${allMessages.size}")
                for (msg in allMessages) {
                    Log.d("ChatDebug", "Message: id=${msg.id}, chat=${msg.chat}, content=${msg.content}, sender=${msg.sender}")
                }
            }

            // This flow operation is safe because Room is configured to run on a background thread
            db.messageDao().getChatMessagesFlow(chatName).collect{ newChatMessages ->
                Log.d("ChatDebug", "Received ${newChatMessages.size} messages for chat: $chatName")

                // update the UI state with the new state
                _uiState.update { prev ->
                    prev.copy(
                        allChatMessages = newChatMessages
                    )
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

    private fun createConversationId(uuid1: String, uuid2: String): String {
        return listOf(uuid1, uuid2).sorted().joinToString("-")
    }

    fun sendChatMessage(virtualAddress: InetAddress, message: String) {
        val sendTime: Long = System.currentTimeMillis()

        val chatNameForMessage = when {
            TestDeviceService.isOnlineTestDevice(virtualAddress) ->
                TestDeviceService.TEST_DEVICE_NAME
            ipStr == TestDeviceService.TEST_DEVICE_IP_OFFLINE ||
                    userEntity?.name == TestDeviceService.TEST_DEVICE_NAME_OFFLINE ->
                TestDeviceService.TEST_DEVICE_NAME_OFFLINE
            else -> userEntity?.name ?: ipStr
        }

        Log.d("ChatDebug", "Sending message to chat: $chatNameForMessage")

        val messageEntity = Message(0, sendTime, message, "Me", chatNameForMessage)

        viewModelScope.launch {
            //save to local database
            db.messageDao().addMessage(messageEntity)

            //update convo with the new message
            if (userEntity != null){
                try {

                    //check if it is a test device and use a correct uuid
                    val userUuid = when {
                        TestDeviceService.isOnlineTestDevice(virtualAddress) ->
                            "test-device-uuid"  // MUST match the UUID in GlobalApp.insertTestConversations()

                        TestDeviceService.isOfflineTestDevice(virtualAddress) ||
                                userEntity.name == TestDeviceService.TEST_DEVICE_NAME_OFFLINE ->
                            "offline-test-device-uuid"  // MUST match the UUID for offline test device

                        else -> userEntity.uuid
                    }

                    Log.d("ChatScreenViewModel", "Using UUID: $userUuid for conversation update")


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