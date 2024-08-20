package com.greybox.projectmesh.views

import android.content.Context
import com.greybox.projectmesh.style.WhiteButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
//import com.greybox.projectmesh.model.HomeScreenModel
//import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.VirtualNode

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_mesh_libmeshrabiya")
@Composable
fun HomeScreen(){
    // val uiState: HomeScreenModel by viewModel.uiState.collectAsState(initial = HomeScreenModel())

    // Request bluetooth permission
    val requestBluetoothPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}

    // Request nearby wifi permission
    val requestNearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}

    // Request location permission
    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}

    // Request fine location permission
    val requestFineLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}
    val context = LocalContext.current
    var thisNode by remember { mutableStateOf( AndroidVirtualNode(
        appContext = context.applicationContext,
        dataStore = context.applicationContext.dataStore
    ) ) }
    val nodes by thisNode.state.collectAsState(LocalNodeState())
    var connectLink by remember { mutableStateOf("")}

    val barcodeEncoder = remember {
        BarcodeEncoder()
    }
    val coroutineScope = rememberCoroutineScope()
    //myNode.setWifiHotspotEnabled(enabled = true, preferredBand = ConnectBand.BAND_5GHZ)
    var displayQrcode by remember { mutableStateOf(false) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }
        Column {
            Text(
                text = "Device IP: ${nodes.address.addressToDotNotation()}",
                style = TextStyle(fontSize = 15.sp)
            )
//            Spacer(modifier = Modifier.height(6.dp))
//            Text(
//                text = "Device UUID: ${thisIDString}",
//                style = TextStyle(fontSize = 15.sp)
//            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                WhiteButton(onClick = {displayQrcode = !displayQrcode},
                    modifier = Modifier.padding(4.dp),
                    text = if (displayQrcode) "Stop Hotspot" else "Start Hotspot",
                    enabled = true)
            }
            // Generating QR CODE
            val connectUri = nodes.connectUri
            if (connectUri != null && displayQrcode) {
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeView(
                    connectUri,
                    nodes.wifiState.connectConfig?.ssid,
                    nodes.wifiState.connectConfig?.passphrase,
                    nodes.wifiState.connectConfig?.bssid,
                    nodes.wifiState.connectConfig?.port.toString())
            }

            // initialize the QR code scanner
            val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) {
                    result ->
                val link = result.contents
                if (link != null) {
                    try{
                        val connectConfig = MeshrabiyaConnectLink.parseUri(link).hotspotConfig
                        if (connectConfig != null){
                            coroutineScope.launch {
                                try {
                                    thisNode.connectAsStation(connectConfig)
                                }
                                catch (e: Exception) {
                                    // Link doesn't have wifi configuration
                                }
                            }
                        }
                    }
                    catch (e: Exception) {
                        // Invalid Link
                    }
                }
            }
            // Scan the QR CODE
            val stationState = nodes.wifiState.wifiStationState
            if (stationState != null){
                Column (modifier = Modifier.fillMaxWidth()){
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Wifi Station (Client) Connection", style = TextStyle(fontSize = 16.sp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row (modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center){
                        WhiteButton(onClick = {
                            qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false)
                                .setPrompt("Scan another device to join the Mesh")
                                .setBeepEnabled(false))},
                            modifier = Modifier.padding(4.dp),
                            text = "Connect via QR Code Scan",
                            enabled = !displayQrcode)
                    }
                }
            }
        }
    }
}

@Composable
fun QRCodeView(qrcodeUrl: String, ssid: String?, password: String?,
               mac: String?, port: String?) {
    Row {
        // QR Code left side, Device info on the right side
        Image(
            painter = rememberQrBitmapPainter(
                content = qrcodeUrl,
                size = 160.dp,
                padding = 1.dp
            ),
            contentDescription = "QR Code"
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "SSID: $ssid")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Password: $password")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "MAC Address: $mac")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Port: $port")
        }
    }
}