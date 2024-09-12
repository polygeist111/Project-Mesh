package com.greybox.projectmesh.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.buttonStyle.WhiteButton
import com.greybox.projectmesh.model.HomeScreenModel
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
    val fileChosen by remember { mutableStateOf(false) }
    val uiState: SendScreenModel by viewModel.uiState.collectAsState(initial = SendScreenModel())
    // File picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){
        //uri -> viewModel.onFileChosen(uri)
    }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted){
            openDocumentLauncher.launch(arrayOf("*/*"))
        }
        else{
            // Handle permission denied
        }
    }

    // Permission check
//    val checkPermission = {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(
//                    this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                openDocumentLauncher.launch(arrayOf("*/*"))
//            } else {
//                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//            }
//        } else {
//            openDocumentLauncher.launch(arrayOf("*/*"))
//        }
//    }



    if (!fileChosen){
        Row(modifier = Modifier.fillMaxSize()) {
            WhiteButton(onClick = { requestPermissionLauncher },
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