// app/src/main/java/com/greybox/projectmesh/messaging/ui/screens/ChatScreen.kt

package com.greybox.projectmesh.messaging.ui.screens

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.DeviceStatusManager
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import com.greybox.projectmesh.messaging.ui.viewmodels.ChatScreenViewModel
import com.greybox.projectmesh.server.AppServer
import com.greybox.projectmesh.views.LongPressCopyableText
import com.greybox.projectmesh.testing.TestDeviceService
import kotlinx.coroutines.runBlocking
import org.kodein.di.compose.localDI
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import android.provider.OpenableColumns
import org.kodein.di.instance
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.greybox.projectmesh.bluetooth.rememberBluetoothConnectLauncher
import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState
import kotlinx.coroutines.launch

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
            vmFactory = { di, savedStateHandle ->
                ChatScreenViewModel(di, savedStateHandle)
            },
            defaultArgs = Bundle().apply {
                putSerializable("virtualAddress", virtualAddress)

            }
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

    // Track device status from DeviceStatusManager
    var deviceStatus by remember { mutableStateOf(false) }

    // Only observe for real devices (not placeholders)
    val shouldTrackStatus = remember {
        virtualAddress.hostAddress != "0.0.0.0" &&
                virtualAddress.hostAddress != TestDeviceService.TEST_DEVICE_IP_OFFLINE
    }

    if (shouldTrackStatus) {
        // Collect directly from DeviceStatusManager
        val statusMap by DeviceStatusManager.deviceStatusMap.collectAsState()

        // Use LaunchedEffect with a key derived from the status map entry for this device
        LaunchedEffect(statusMap[virtualAddress.hostAddress]) {
            val newStatus = statusMap[virtualAddress.hostAddress] ?: false
            if (deviceStatus != newStatus) {
                Log.d(
                    "ChatScreen",
                    "Device status changed: ${virtualAddress.hostAddress} is now ${if (newStatus) "online" else "offline"}"
                )
                deviceStatus = newStatus
            }
        }
    }

    //Grab AppServer fromm local DI
    val di = localDI()
    val appServer: AppServer by di.instance()

    val contextMessageForFile = LocalContext.current

    // set up the system file-picker
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // 1) start the transfer
            appServer.addOutgoingTransfer(selectedUri, virtualAddress)

            //Get name of file
            var displayName = "unknown"
            contextMessageForFile.contentResolver
                .query(selectedUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            .takeIf { it >= 0 }
                            ?.let { idx ->
                                displayName = cursor.getString(idx)
                            }
                    }
                }
            //Send a chat message when file is sent via file transfer
            viewModel.sendChatMessage(
                virtualAddress,
                "File $displayName was sent",
                null
            )
        }
    }
    // declare the UI state, we can use the uiState to access the current state of the viewModel
    val uiState: ChatScreenModel by viewModel.uiState.collectAsState(initial = ChatScreenModel())
    var textMessage by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    val scrollState = rememberScrollState()

    // bluetooth only flag
    val btOnly by viewModel.btOnlyFlag.collectAsState()
    // boolean use to toggle enabling/disabling chat
    val canSend = deviceStatus || btOnly


// Bluetooth launcher, allows device selection
    val bluetoothLauncher = rememberBluetoothConnectLauncher { result ->
        val macAddress = result.macAddress
        val deviceName = result.deviceName

        if (macAddress != null) {
            viewModel.linkBluetoothDevice(macAddress)

            Log.d("ChatScreen", "Selected device: $deviceName ($macAddress)")

        } else {
            Log.d("ChatScreen", "No Bluetooth device selected...device = null")
        }
    }





    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)) {

            //add a status bar at the top of the chat
            UserStatusBar(
                userName = userInfo,
                isOnline = deviceStatus,
                userAddress = virtualAddress.hostAddress
            )

            // show a link device button when entering bluetooth only mode
            if (btOnly){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {bluetoothLauncher.launch(MeshrabiyaBluetoothState()) }
                    ) {
                        Text("Link Bluetooth Device")
                    }
                }
            }

            // Show device status indicator
            if (!deviceStatus && !btOnly) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
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

            DisplayAllMessages(uiState, onClickButton)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(4.dp),
            verticalAlignment = Alignment.Bottom //Change the Alignment So that it does not
            // increase the height together with the TextField and be floating in the middle of the
            // screen
        ) {

            //File Picker Button
            IconButton(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
                    .size(40.dp),
                onClick = { pickFileLauncher.launch("*/*") }
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Select a file to send"
                )
            }
            TextField(
                modifier = Modifier
                    .weight(3f)
                    .heightIn(min = 40.dp, max = 120.dp) // Added Min and Max Height to the
                    // TextField
                    .verticalScroll(scrollState),
                value = textMessage,
                onValueChange = {
                    textMessage = it
                },
                maxLines = 5,
                enabled = canSend // disable text input when user is offline
            )
            Button(modifier = Modifier.weight(1f), onClick = {
                val message = textMessage.trimEnd()
                //val imgpath = "sdcard/padorubastard.jpg"//test image path
                //future implementation should implement file picker
                //val filepath = Uri.parse(imgpath)
                if(message.isNotEmpty()) {
                    if (btOnly) {
                        // Send via Bluetooth
                        viewModel.sendBluetoothChatMessage(message, null)
                    } else {
                        // Send via WiFi
                        viewModel.sendChatMessage(virtualAddress, message, null)
                    }
                    textMessage = ""
                }
            },
                enabled = canSend  //disable button when user is offline
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

    //track if messages are showing:
    val hasMessages = uiState.allChatMessages.isNotEmpty()

    LaunchedEffect(uiState.allChatMessages.size) {
        Log.d("ChatScreen", "DisplayAllMessages with ${uiState.allChatMessages.size} messages")
    }

    LazyColumn{
        if (!hasMessages){
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No messages yet. Start a conversation!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.offlineWarning != null) {
                        Text(
                            text = "You're currently offline. Any messages you send will be delivered when connection is restored.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
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
    val context = LocalContext.current

    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = if(sentBySelf){
                Color.Cyan
            }else{
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = modifier
            .padding(10.dp)
            .widthIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = sender,
                style = MaterialTheme.typography.labelMedium
            )

            messageContent()
            //ONLY SHOW FILE ATTACHMENT IF PRESENT
            if (chatMessage.file != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "File",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = chatMessage.file.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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

