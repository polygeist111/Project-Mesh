package com.greybox.projectmesh.views

import android.Manifest
import android.os.Build
import com.greybox.projectmesh.buttonStyle.WhiteButton
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.NEARBY_WIFI_PERMISSION_NAME
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.greybox.projectmesh.model.HomeScreenModel
import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.components.ConnectWifiLauncherStatus
import com.greybox.projectmesh.components.meshrabiyaConnectLauncher
import com.greybox.projectmesh.components.ConnectWifiLauncherResult
import com.greybox.projectmesh.hasBluetoothConnectPermission
import com.greybox.projectmesh.hasNearbyWifiDevicesOrLocationPermission

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel(
    factory = ViewModelFactory(
        di = localDI(),
        owner = LocalSavedStateRegistryOwner.current,
        vmFactory = { HomeScreenViewModel(it) },
        defaultArgs = null)))
{
    val di = localDI()
    val uiState: HomeScreenModel by viewModel.uiState.collectAsState(initial = HomeScreenModel())
    val node: VirtualNode by di.instance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Request bluetooth permission
    val requestBluetoothPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){granted -> if (granted){
        viewModel.onSetIncomingConnectionsEnabled(true)
    } }

    // Request nearby wifi permission
    val requestNearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ granted -> if (granted){
        if(context.hasBluetoothConnectPermission()){
            viewModel.onSetIncomingConnectionsEnabled(true)
        }
        else if(Build.VERSION.SDK_INT >= 31){
            requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    } }

    HomeScreenView(
        uiState = uiState,
        node = node as AndroidVirtualNode,
        onConnectWifiLauncherResult = { result ->
            if (result.hotspotConfig != null) {
                viewModel.onConnectWifi(result.hotspotConfig)
            }
        },
        onSetIncomingConnectionsEnabled = {enabled ->
            if(enabled && !context.hasNearbyWifiDevicesOrLocationPermission()){
                requestNearbyWifiPermissionLauncher.launch(NEARBY_WIFI_PERMISSION_NAME)
            }
            else if(enabled && !context.hasBluetoothConnectPermission() && Build.VERSION.SDK_INT >= 31){
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
            else{
                viewModel.onSetIncomingConnectionsEnabled(enabled)
            }
        }
    )
}

@Composable
fun HomeScreenView(
    uiState: HomeScreenModel,
    node: AndroidVirtualNode,
    onConnectWifiLauncherResult: (ConnectWifiLauncherResult) -> Unit,
    onSetIncomingConnectionsEnabled: (Boolean) -> Unit = { },
){
    var connectLauncherState by remember {
        mutableStateOf(ConnectWifiLauncherStatus.INACTIVE)
    }
    val di = localDI()
    val barcodeEncoder = remember { BarcodeEncoder() }
    val coroutineScope = rememberCoroutineScope()
    //myNode.setWifiHotspotEnabled(enabled = true, preferredBand = ConnectBand.BAND_5GHZ)
    var displayQrcode by remember { mutableStateOf(false) }
    val connectLauncher = meshrabiyaConnectLauncher(
        node = node,
        onStatusChange = {connectLauncherState = it},
        onResult = onConnectWifiLauncherResult
    )
    // initialize the QR code scanner
    val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) {
            result ->
        // Get the contents of the QR code
        val link = result.contents
        if (link != null) {
            try{
                // Parse the link, get the wifi connect configuration.
                val hotSpot = MeshrabiyaConnectLink.parseUri(
                    uri=link,
                    json=di.direct.instance()
                ).hotspotConfig
                // if the configuration is valid, connect to the device.
                if (hotSpot != null){
                    connectLauncher.launch(hotSpot)
                }
                else{
                    // Link doesn't have a connect config
                }
            }
            catch (e: Exception) {
                // Invalid Link
            }
        }
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Column {
            Text(
                text = "Device IP: ${uiState.localAddress.addressToDotNotation()}",
                style = TextStyle(fontSize = 15.sp)
            )
//            Spacer(modifier = Modifier.height(6.dp))
//            Text(
//                text = "Device UUID: ${uiState.wifiState?.connectConfig}",
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
                if (displayQrcode){
                    onSetIncomingConnectionsEnabled(true)
                }
                else{
                    onSetIncomingConnectionsEnabled(false)
                }
            }

            // Generating QR CODE
            val connectUri = uiState.connectUri
            if (connectUri != null && displayQrcode) {
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeView(
                    connectUri,
                    uiState.wifiState?.connectConfig?.ssid,
                    uiState.wifiState?.connectConfig?.passphrase,
                    uiState.wifiState?.connectConfig?.bssid,
                    uiState.wifiState?.connectConfig?.port.toString())
            }
            // Scan the QR CODE
            val stationState = uiState.wifiState?.wifiStationState
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