package com.greybox.projectmesh.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.model.NetworkScreenModel
import com.greybox.projectmesh.viewModel.NetworkScreenViewModel
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import org.kodein.di.compose.localDI

@Composable
fun NetworkScreen(viewModel: NetworkScreenViewModel = viewModel(
    factory = ViewModelFactory(
        di = localDI(),
        owner = LocalSavedStateRegistryOwner.current,
        vmFactory = { NetworkScreenViewModel(it) },
        defaultArgs = null))
) {
    val uiState: NetworkScreenModel by viewModel.uiState.collectAsState(initial = NetworkScreenModel())

    // display all the wifi nodes
    LazyColumn{
        items(
            items = uiState.nodes.entries.toList(),
            key = {it.key}
        ){ eachItem ->
            WifiListItem(eachItem.key, eachItem.value, onClick = {})
        }
    }
}

@Composable
fun WifiListItem(
    wifiAddress: Int,
    wifiEntry: VirtualNode.LastOriginatorMessage,
    onClick: () -> Unit,
){
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(wifiAddress.addressToDotNotation()) },
        supportingContent = {
            Text("Ping: ${wifiEntry.originatorMessage.pingTimeSum}ms " +
            "Hops: ${wifiEntry.hopCount} ")
        },
    )
}