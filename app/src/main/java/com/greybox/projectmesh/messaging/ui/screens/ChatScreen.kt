package com.greybox.projectmesh.messaging.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import com.greybox.projectmesh.messaging.ui.viewmodels.ChatScreenViewModel
import com.greybox.projectmesh.views.LongPressCopyableText
import org.kodein.di.compose.localDI
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.messaging.data.entities.Message
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.kodein.di.Copy
import org.kodein.di.DI
import kotlin.io.path.moveTo
import com.greybox.projectmesh.user.UserRepository
import com.greybox.projectmesh.user.UserDao
import com.greybox.projectmesh.user.UserEntity

@Composable
fun ChatScreen(
    virtualAddress: InetAddress,
    onClickButton: () -> Unit,
    viewModel: ChatScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { di -> ChatScreenViewModel(virtualAddress, di) },
            defaultArgs = null
        )
    )
) {
    // declare the UI state, we can use the uiState to access the current state of the viewModel
    val uiState by viewModel.uiState.collectAsState()
    var textMessage by rememberSaveable { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)) {
//            Button(onClick = onClickButton, modifier = Modifier.fillMaxWidth()) {
//                Text(text = "Ping")
//            }
            DisplayAllMessages(uiState, onClickButton)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(4.dp)) {
            TextField(modifier = Modifier.weight(3f),
                value = textMessage,
                onValueChange = {
                    textMessage = it
                })
            Button(modifier = Modifier.weight(1f), onClick = {
                val message = textMessage.trimEnd()
                if(message.isNotEmpty()) {
                    viewModel.sendChatMessage(message)
                    // resets the text field
                    textMessage = ""
                }
            }) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun DisplayAllMessages(uiState: ChatScreenModel, onClickButton: () -> Unit) {
    val context = LocalContext.current
    // display all chat messages
    LazyColumn{
        item{
            Row(modifier = Modifier.fillMaxWidth()){
                Text(modifier = Modifier.weight(3f),
                    text = "Device name: ${uiState.deviceName}, IP address: ${uiState.virtualAddress.hostAddress}"
                )
                Button(modifier = Modifier.weight(1f),
                    onClick = onClickButton) {
                    Text(text = "Ping")
                }
            }
        }
        items( // todo
            items = uiState.allChatMessages
        ){ chatMessage ->
            val sender: String = if (chatMessage.sender == "Me") {
                "Me"
            } else {
                val ipStr = uiState.virtualAddress.hostAddress
                val user = runBlocking {
                    GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipStr)
                }
                user?.name ?: "Loading..."
            }

            val sentBySelf = chatMessage.sender == "Me"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (sentBySelf) Arrangement.End else Arrangement.Start
            ) {
                MessageBubble(chatMessage = chatMessage, messageContent = {
                    LongPressCopyableText(
                        context = context,
                        text = "",
                        //text = "$sender [${SimpleDateFormat("HH:mm").format(Date(chatMessage.dateReceived))}]: ",
                        textCopyable = chatMessage.content,
                        textSize = 15
                    )
                }, sender = sender, modifier = Modifier,)
            }
        }
    }
}



//@Composable
//fun MessageBubble(chatMessage: Message, messageContent: @Composable () -> Unit, sender: String, modifier: Modifier, sentBySelf: Boolean) {
//    val bubbleColor = MaterialTheme.colorScheme.surfaceVariant
//    val stemWidth = LocalDensity.current.run { 20.dp.toPx() }
//    val stemHeight = LocalDensity.current.run { 30.dp.toPx() }
//    val bubbleWidth = LocalDensity.current.run { 240.dp.toPx() }
//    val bubbleHeight = LocalDensity.current.run { 100.dp.toPx() }
//    val cornerRadius = LocalDensity.current.run { 16.dp.toPx() }
//    Canvas(
//        modifier = modifier
//            .size(width = 240.dp, height = 100.dp)
//            .padding(4.dp)
//    ) {
//        val bubblePath = Path().apply {
//            if (sentBySelf) {
//                // Right-aligned bubble
//                moveTo(bubbleWidth - cornerRadius, 0f)
//                lineTo(bubbleWidth - stemWidth, 0f)
//                lineTo(bubbleWidth, stemHeight / 2)
//                lineTo(bubbleWidth - stemWidth, stemHeight)
//                lineTo(bubbleWidth - cornerRadius, stemHeight)
//                // Top right corner
//                arcTo(
//                    rect = androidx.compose.ui.geometry.Rect(
//                        offset = Offset(bubbleWidth - cornerRadius * 2, 0f),
//                        size = Size(cornerRadius * 2, cornerRadius * 2)
//                    ),
//                    startAngleDegrees = 270f,
//                    sweepAngleDegrees = 90f,
//                    forceMoveTo = false
//                )
//                // Top left corner
//                arcTo(
//                    rect = androidx.compose.ui.geometry.Rect(
//                        offset = Offset(0f, 0f),
//                        size = Size(cornerRadius * 2, cornerRadius * 2)
//                    ),
//                    startAngleDegrees = 180f ,
//                    sweepAngleDegrees = 90f,
//                    forceMoveTo = false
//                )
//                // Bottom left corner
//                arcTo(
//                    rect = Rect(
//                        offset = Offset(0f, bubbleHeight - cornerRadius * 2),
//                        size = Size(cornerRadius * 2, cornerRadius * 2)
//                    ),
//                    startAngleDegrees = 90f,
//                    sweepAngleDegrees = 90f,
//                    forceMoveTo = false
//                )
//            }
//        }
//    }
//}
////        if (sentBySelf) {
////            // Right-aligned bubble
////            moveTo(bubbleWidth - cornerRadius, 0f)
////            lineTo(bubbleWidth - stemWidth, 0f)
////            lineTo(bubbleWidth, stemHeight / 2)
////            lineTo(bubbleWidth - stemWidth, stemHeight)
////            lineTo(bubbleWidth - cornerRadius, stemHeight)
////    }
@Composable
fun MessageBubble(chatMessage: Message, messageContent: @Composable () -> Unit, sender: String, modifier: Modifier){
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
//            Text(
//                text = "",
//                //text = "$sender [${SimpleDateFormat("HH:mm").format(Date(chatMessage.dateReceived))}]: ",
//                style = MaterialTheme.typography.labelSmall,
//                textAlign = TextAlign.End
//            )
        }
    }
}

//TESTING CODE//
/*
val dummyDI = DI() {
}

// Dummy ViewModel for Preview
class PreviewChatScreenViewModel(virtualAddress: InetAddress) : ChatScreenViewModel(virtualAddress, dummyDI) {
    private val _uiState = MutableStateFlow(
        ChatScreenModel(
            virtualAddress = virtualAddress,
            deviceName = "Preview Device",
            allChatMessages = listOf(
                Message(sender = "Me", content = "Hello!", id = 1, dateReceived = System.currentTimeMillis(), chat = "abs"),
                Message(sender = "Other", content = "Hi there!", id = 2,dateReceived = System.currentTimeMillis(), chat = "abs"),
                Message(sender = "Me", content = "How are you?", id = 3,dateReceived = System.currentTimeMillis(), chat = "abs"),
            )
        )
    )
    override val uiState: StateFlow<ChatScreenModel> = _uiState

    override fun sendChatMessage(message: String) {
    }
}

// Preview Function
@Preview(showBackground = true, name = "Chat Screen Preview")
@Composable
fun ChatScreenPreview() {
    val dummyAddress = InetAddress.getByName("192.168.1.1") // Replace with a dummy IP
    val viewModel = PreviewChatScreenViewModel(dummyAddress)
    ChatScreen(
        virtualAddress = dummyAddress,
        onClickButton = { println("Ping button clicked in preview") },
        viewModel = viewModel
    )
}
*/