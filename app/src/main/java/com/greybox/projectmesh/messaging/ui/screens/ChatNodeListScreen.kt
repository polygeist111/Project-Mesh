package com.greybox.projectmesh.messaging.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.model.NetworkScreenModel
import com.greybox.projectmesh.viewModel.NetworkScreenViewModel
import com.greybox.projectmesh.views.WifiListItem
import org.kodein.di.compose.localDI
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner

@Composable
fun ChatNodeListScreen(
    onNodeSelected: (String) -> Unit,
    // Reuse NetworkScreenViewModel in the same way as NetworkScreen
    viewModel: NetworkScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { NetworkScreenViewModel(it) },
            defaultArgs = null
        )
    )
) {
    // Provide the same initial state you used in NetworkScreen
    val uiState: NetworkScreenModel by viewModel.uiState.collectAsState(initial = NetworkScreenModel())

    LazyColumn {
        items(
            items = uiState.allNodes.entries.toList(),
            key = { it.key }
        ) { nodeEntry ->
            // Tapping a WifiListItem calls 'onNodeSelected' instead of onClickNetworkNode
            WifiListItem(
                wifiAddress = nodeEntry.key,
                wifiEntry = nodeEntry.value,
                onClick = { selectedIp ->
                    onNodeSelected(selectedIp)
                }
            )
        }
    }
}