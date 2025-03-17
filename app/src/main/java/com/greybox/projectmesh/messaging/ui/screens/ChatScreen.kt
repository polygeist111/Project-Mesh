package com.greybox.projectmesh.messaging.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import com.greybox.projectmesh.messaging.ui.viewmodels.ChatScreenViewModel
import com.greybox.projectmesh.views.LongPressCopyableText
import com.greybox.projectmesh.testing.TestDeviceService
import kotlinx.coroutines.runBlocking
import org.kodein.di.compose.localDI
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun ChatScreen(
    virtualAddress: InetAddress,
    userName: String? = null,
    isOffline: Boolean = false,
    onClickButton: () -> Unit,
    viewModel: ChatScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { ChatScreenViewModel(it, virtualAddress) },
            defaultArgs = null
        )
    )
) {
    //get user info
    val userInfo = remember {
        if (userName != null) {
            //use provided userName if available
            userName
        } else {
            //else try to look it up from the repository
            runBlocking {
                GlobalApp.GlobalUserRepo.userRepository.getUserByIp(virtualAddress.hostAddress)?.name
                    ?: "Unknown User"
            }
        }
    }

    val isUserOnline = remember {
        if (!isOffline) {
            //if not explicitly marked as offline, check if test device or has valid IP
            TestDeviceService.isOnlineTestDevice(virtualAddress) ||
                    (virtualAddress.hostAddress != "0.0.0.0" &&
                            virtualAddress.hostAddress != TestDeviceService.TEST_DEVICE_IP_OFFLINE)
        } else {
            //use explicitly provided offline status
            false
        }
    }

    // declare the UI state, we can use the uiState to access the current state of the viewModel
    val uiState: ChatScreenModel by viewModel.uiState.collectAsState(initial = ChatScreenModel())
    var textMessage by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)) {


            //add a status bar at the top of the chat
            UserStatusBar(
                userName = userInfo,
                isOnline = isUserOnline,
                userAddress = virtualAddress.hostAddress
            )

            DisplayAllMessages(uiState, onClickButton)

            //add an offline indicator if user is offline
            if(!isUserOnline){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors (
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Icon(
                            imageVector = Icons.Default.SignalWifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User is offline. Messages cannot be sent.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(4.dp)) {//attach a button for connecting to file picker

            TextField(
                modifier = Modifier.weight(3f),
                value = textMessage,
                onValueChange = {
                    textMessage = it
                },
                enabled = isUserOnline // disable text input when user is offline
            )
            Button(modifier = Modifier.weight(1f), onClick = {
                val message = textMessage.trimEnd()
                if(message.isNotEmpty()) {
                    viewModel.sendChatMessage(virtualAddress, message)
                    // resets the text field
                    textMessage = ""
                }
            },
                enabled = isUserOnline //diable button when user is offline
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun UserStatusBar(
    userName: String,
    isOnline: Boolean,
    userAddress: String
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //User avatar/icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.first().toString(),
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            //User name and status
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) Color.Green else Color.Gray
                            )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Status text
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // IP address
                    Text(
                        text = userAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayAllMessages(uiState: ChatScreenModel, onClickButton: () -> Unit) {
    val context = LocalContext.current
    // display all chat messages
    Log.d("ChatDebug", "DisplayAllMessages called with ${uiState.allChatMessages.size} messages")

    LazyColumn{
        item{
            Row(modifier = Modifier.fillMaxWidth()){
                Text(modifier = Modifier.weight(3f),
                    text = "Device name: ${uiState.deviceName}, IP address: ${uiState.virtualAddress.hostAddress}"
                )
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onClickButton
                ) {
                    Text(text = "Ping")
                }
            }
        }
        if (uiState.allChatMessages.isEmpty()){
            item {
                Text (
                    text = "No messages yet. Start a conversation!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        items(
            items = uiState.allChatMessages
        ){ chatMessage ->
            Log.d("ChatDebug", "Rendering message: ${chatMessage.content}")
            val sender: String = if (chatMessage.sender == "Me") {
                "Me"
            } else {
                val ipStr = uiState.virtualAddress.hostAddress
                val user = runBlocking {
                    GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)
                }
                user?.name ?: chatMessage.sender
            }

            val sentBySelf = chatMessage.sender == "Me"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (sentBySelf) Arrangement.End else Arrangement.Start
            ) {
                MessageBubble(
                    chatMessage = chatMessage,
                    sentBySelf = sentBySelf,
                    messageContent = {
                        LongPressCopyableText(
                            context = context,
                            text = "",
                            textCopyable = chatMessage.content,
                            textSize = 15
                        )
                    },
                    sender = sender,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    chatMessage: Message,
    sentBySelf: Boolean,
    messageContent: @Composable () -> Unit,
    sender: String,
    modifier: Modifier
){
    ElevatedCard(
        //backgroundColor: color = MaterialTheme.colorScheme.surfaceVariant,
        colors = CardDefaults.cardColors(
            containerColor = if(sentBySelf){
                Color.Cyan
            }else{
                MaterialTheme.colorScheme.surfaceVariant

            }
        ),
        modifier = modifier
            .size(width = 200.dp, height = 100.dp)
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = sender,
                style = MaterialTheme.typography.labelMedium
            )
            messageContent()

            // Adding timestamp to bottom right of message bubble
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm").format(Date(chatMessage.dateReceived)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
