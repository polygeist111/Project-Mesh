package com.greybox.projectmesh.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.model.ReceiveScreenModel
import com.greybox.projectmesh.server.AppServer
import com.greybox.projectmesh.viewModel.ReceiveScreenViewModel
import org.kodein.di.compose.localDI

@Composable
fun ReceiveScreen(
    viewModel: ReceiveScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { ReceiveScreenViewModel(it) },
            defaultArgs = null,
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState(ReceiveScreenModel())
    HandleIncomingTransfers(
        uiState = uiState,
        onAccept = viewModel::onAccept,
        onDecline = viewModel::onDecline,
        onDelete = viewModel::onDelete,
    )
}

@Composable
fun HandleIncomingTransfers(
    uiState: ReceiveScreenModel,
    onAccept: (AppServer.IncomingTransferInfo) -> Unit = {},
    onDecline: (AppServer.IncomingTransferInfo) -> Unit = {},
    onDelete: (AppServer.IncomingTransferInfo) -> Unit = {},
){
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = uiState.incomingTransfers,
            key = {Triple(it.fromHost, it.id, it.requestReceivedTime)}
        ){ transfer ->
            ListItem(
                modifier = Modifier
                    .clickable {
                        // viewModel.openFile(transfer)
                    }
                    .fillMaxWidth(),

                headlineContent = {
                    Text(transfer.name)
                },
                supportingContent = {
                    Column{
                        Text("<- ${transfer.fromHost.hostAddress} ")
                        Text("Status: ${transfer.status}")
                        Text("${autoConvertByte(transfer.transferred)} / ${autoConvertByte(transfer.size)}")
                        if(transfer.status == AppServer.Status.PENDING){
                            Row{
                                IconButton(onClick = {onAccept(transfer)},
                                    modifier = Modifier.width(100.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = {onDecline(transfer)},
                                    modifier = Modifier.width(100.dp)) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Decline")
                                }
                            }
                        }
                    }
                },
                trailingContent = {
                    if(transfer.status == AppServer.Status.COMPLETED){
                        IconButton(onClick = {onDelete(transfer)}) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    }
}