package com.greybox.projectmesh.messaging.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.messaging.network.MessageService
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.net.InetAddress
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatScreenViewModel(
    private val virtualAddress: InetAddress,
    override val di: DI
) : ViewModel(), DIAware {
    private val messageService: MessageService by di.instance()

    private val _uiState = MutableStateFlow(
        ChatScreenModel(
            deviceName = "My Device", // Or get from preferences
            virtualAddress = virtualAddress,
            allChatMessages = emptyList()
        )
    )
    val uiState: StateFlow<ChatScreenModel> = _uiState.asStateFlow()

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            val newMessage = Message(
                id = 0,
                dateReceived = System.currentTimeMillis(),
                content = message,
                sender = "Me",
                chat = virtualAddress.hostAddress
            )

            messageService.sendMessage(virtualAddress, newMessage)
        }
    }
}