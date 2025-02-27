package com.greybox.projectmesh.messaging.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.ui.models.ChatScreenModel
import com.greybox.projectmesh.messaging.ui.viewmodels.ChatScreenViewModel
import com.greybox.projectmesh.views.LongPressCopyableText
import kotlinx.coroutines.runBlocking
import org.kodein.di.compose.localDI
import java.net.InetAddress


@Composable
fun ChatScreen(
    virtualAddress: InetAddress,
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
    // declare the UI state, we can use the uiState to access the current state of the viewModel
    val uiState: ChatScreenModel by viewModel.uiState.collectAsState(initial = ChatScreenModel())
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
            .padding(4.dp)) {//attach a button for connecting to file picker

            TextField(modifier = Modifier.weight(3f),
                value = textMessage,
                onValueChange = {
                    textMessage = it
                })
            Button(modifier = Modifier.weight(1f), onClick = {
                val message = textMessage.trimEnd()
                if(message.isNotEmpty()) {
                    viewModel.sendChatMessage(virtualAddress, message)
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
                MessageBubble(chatMessage = chatMessage, sentBySelf = sentBySelf, messageContent = {
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

@Composable
fun MessageBubble(chatMessage: Message, sentBySelf: Boolean, messageContent: @Composable () -> Unit, sender: String, modifier: Modifier){
    ElevatedCard(
        //backgroundColor: color = MaterialTheme.colorScheme.surfaceVariant,
        colors = CardDefaults.cardColors(
            containerColor = if(sentBySelf){
                Color.Cyan
            }else{
                MaterialTheme.colorScheme.surfaceVariant

            }
//            backgroundColor: Color =
//            //containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
