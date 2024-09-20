package com.greybox.projectmesh.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.greybox.projectmesh.buttonStyle.WhiteButton
import com.greybox.projectmesh.model.SendScreenModel
import com.greybox.projectmesh.viewModel.SendScreenViewModel
import org.kodein.di.compose.localDI

@Composable
fun SendScreen(
    viewModel: SendScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { SendScreenViewModel(it) },
            defaultArgs = null,)
    )
)
{
    // declare the UI state
    val uiState: SendScreenModel by viewModel.uiState.collectAsState(initial = SendScreenModel())
    // indicate a file has been chosen
    var isFileChosen = uiState.fileUri != null
    // File picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){ uri ->
        if (uri != null){
            viewModel.onFileChosen(uri)
        }
    }

    if (!isFileChosen){
        Row(modifier = Modifier.fillMaxSize()) {
            WhiteButton(onClick = {
                openDocumentLauncher.launch(arrayOf("*/*"))
                isFileChosen = true
            },
                modifier = Modifier.align(Alignment.Bottom).padding(16.dp),
                text = "Send File",
                enabled = true)
        }

    }
    else{
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)){
            Text("Who do you want to send to?")
            // display all the available nodes
            //displayNodes()

        }
    }

}

//@Composable
//// Display all the available nodes
//fun displayNodes(
//    uiState: SendScreenModel,
//){
//    LazyColumn {
//        items(
//            items = uiState.pendingTransfers,
//            key = { it.id }
//        ) {transfer ->
//            ListItem(
//                headlineContent = {
//                    Text("${transfer.name} -> ${transfer.toHost.hostAddress}")
//                },
//                supportingContent = {
//                    Text("Status: ${transfer.status} Sent ${transfer.transferred} / ${transfer.size}")
//                }
//            )
//        }
//    }
//}