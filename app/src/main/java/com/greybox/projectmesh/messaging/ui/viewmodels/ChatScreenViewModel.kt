package com.greybox.projectmesh.messaging.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kodein.di.DI
import com.greybox.projectmesh.server.AppServer
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.instance
import java.net.InetAddress

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
    private val chatName: String = userEntity?.name ?: ipStr
    private val addressDotNotation = virtualAddress.requireAddressAsInt().addressToDotNotation()
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
            // collect the state flow of the AndroidVirtualNode
            db.messageDao().getChatMessagesFlow(chatName).collect{
                    newChatMessages: List<Message> ->

                // update the UI state with the new state
                _uiState.update { prev ->
                    prev.copy(
                        allChatMessages = newChatMessages
                    )
                }
            }
        }
    }

    fun sendChatMessage(virtualAddress: InetAddress, message: String) {
        val sendTime: Long = System.currentTimeMillis()
        viewModelScope.launch {
            db.messageDao().addMessage(Message(0, sendTime, message, "Me", chatName))
        }
        appServer.sendChatMessage(virtualAddress, sendTime, message)
    }
}