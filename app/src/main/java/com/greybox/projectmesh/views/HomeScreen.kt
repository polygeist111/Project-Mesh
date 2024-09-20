package com.greybox.projectmesh.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.greybox.projectmesh.NEARBY_WIFI_PERMISSION_NAME
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.buttonStyle.WhiteButton
//import com.greybox.projectmesh.components.ConnectWifiLauncherResult
//import com.greybox.projectmesh.components.ConnectWifiLauncherStatus
//import com.greybox.projectmesh.components.meshrabiyaConnectLauncher
import com.greybox.projectmesh.hasNearbyWifiDevicesOrLocationPermission
import com.greybox.projectmesh.model.HomeScreenModel
import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance

@Composable
// We customize the viewModel since we need to inject dependencies
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

    // Request nearby wifi permission
    val requestNearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ granted -> if (granted){
        if(context.hasNearbyWifiDevicesOrLocationPermission()){
            viewModel.onSetIncomingConnectionsEnabled(true)
        }
    } }

    // Launch the home screen
    StartHomeScreen(
        uiState = uiState,
        node = node as AndroidVirtualNode,
        onSetIncomingConnectionsEnabled = {enabled ->
            if(enabled && !context.hasNearbyWifiDevicesOrLocationPermission()){
                requestNearbyWifiPermissionLauncher.launch(NEARBY_WIFI_PERMISSION_NAME)
            }
            else{
                viewModel.onSetIncomingConnectionsEnabled(enabled)
            }
        },
        onClickDisconnectWifiStation = viewModel::onClickDisconnectStation,
    )
}

// Display the home screen
@Composable
fun StartHomeScreen(
    uiState: HomeScreenModel,
    node: AndroidVirtualNode,
    onSetIncomingConnectionsEnabled: (Boolean) -> Unit = { },
    onClickDisconnectWifiStation: () -> Unit = { },
    viewModel: HomeScreenViewModel = viewModel(),
){
    val di = localDI()
    val barcodeEncoder = remember { BarcodeEncoder() }
    // initialize the QR code scanner
    val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        // Get the contents of the QR code
        val link = result.contents
        if (link != null) {
            try {
                // Parse the link, get the wifi connect configuration.
                val hotSpot = MeshrabiyaConnectLink.parseUri(
                    uri = link,
                    json = di.direct.instance()
                ).hotspotConfig
                // if the configuration is valid, connect to the device.
                if (hotSpot != null) {
                    // Connect device thru wifi connection
                    viewModel.onConnectWifi(hotSpot)
                } else {
                    // Link doesn't have a connect config
                }
            } catch (e: Exception) {
                // Invalid Link
            }
        }
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Column {
            // Display the device IP
            Text(
                text = "Device IP: ${uiState.localAddress.addressToDotNotation()}",
                style = TextStyle(fontSize = 15.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Display the "Start Hotspot" button
            if (!uiState.wifiConnectionsEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    WhiteButton(
                        onClick = { onSetIncomingConnectionsEnabled(true) },
                        modifier = Modifier.padding(4.dp),
                        text = "Start Hotspot",
                        enabled = true
                    )
                }
            }
            // Display the "Stop Hotspot" button
            else{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    WhiteButton(
                        onClick = { onSetIncomingConnectionsEnabled(false) },
                        modifier = Modifier.padding(4.dp),
                        text = "Stop Hotspot",
                        enabled = true
                    )
                }
            }

            // Generating QR CODE
            val connectUri = uiState.connectUri
            if (connectUri != null && uiState.wifiConnectionsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeView(
                    connectUri,
                    barcodeEncoder,
                    uiState.wifiState?.connectConfig?.ssid,
                    uiState.wifiState?.connectConfig?.passphrase,
                    uiState.wifiState?.connectConfig?.bssid,
                    uiState.wifiState?.connectConfig?.port.toString())
            }
            // Scan the QR CODE
            val stationState = uiState.wifiState?.wifiStationState
            // If the stationState is not null and its status is INACTIVE,
            // It will display the option to connect via a QR code scan.
            if (stationState != null){
                if (stationState.status == WifiStationState.Status.INACTIVE){
                    Column (modifier = Modifier.fillMaxWidth()){
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Wifi Station (Client) Connection", style = TextStyle(fontSize = 16.sp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row (modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center)
                        {
                            WhiteButton(onClick = {
                                qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false)
                                    .setPrompt("Scan another device to join the Mesh")
                                    .setBeepEnabled(true)
                                )},
                                modifier = Modifier.padding(4.dp),
                                text = "Connect via QR Code Scan",
                                enabled = true)
                        }
                    }
                }
                // If the stationState is not INACTIVE, it displays a ListItem that represents
                // the current connection status.
                else{
                    ListItem(
                        headlineContent = {
                            Text(stationState.config?.ssid ?: "(Unknown SSID)")
                        },
                        supportingContent = {
                            Text(
                                (stationState.config?.nodeVirtualAddr?.addressToDotNotation() ?: "") +
                                        " - ${stationState.status}"
                            )
                        },
                        leadingContent = {
                            if(stationState.status == WifiStationState.Status.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp)
                                )
                            }else {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "",
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    onClickDisconnectWifiStation()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Disconnect",
                                )
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // add a Hot Spot status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Hot Spot Status: ${if (uiState.hotspotStatus) "Online" else "Offline"}")
                Spacer(modifier = Modifier.width(8.dp)) // Adds some space between text and dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (uiState.hotspotStatus) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// display the QR code
@Composable
fun QRCodeView(qrcodeUri: String, barcodeEncoder: BarcodeEncoder, ssid: String?, password: String?,
               mac: String?, port: String?) {
    val qrCodeBitMap = remember(qrcodeUri) {
        barcodeEncoder.encodeBitmap(
            qrcodeUri, BarcodeFormat.QR_CODE, 400, 400
        ).asImageBitmap()
    }
    Row {
        // QR Code left side, Device info on the right side
        Image(
            bitmap = qrCodeBitMap,
            contentDescription = "QR Code"
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
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