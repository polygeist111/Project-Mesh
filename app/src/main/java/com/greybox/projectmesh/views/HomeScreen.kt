package com.greybox.projectmesh.views

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.greybox.projectmesh.extension.NEARBY_WIFI_PERMISSION_NAME
import com.greybox.projectmesh.R
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.extension.hasNearbyWifiDevicesOrLocationPermission
import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import androidx.compose.runtime.State
import com.greybox.projectmesh.bluetooth.rememberBluetoothConnectLauncher
import com.greybox.projectmesh.extension.hasStaApConcurrency
import com.greybox.projectmesh.ui.theme.TransparentButton
import com.greybox.projectmesh.viewModel.HomeScreenModel
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.greybox.projectmesh.components.ConnectWifiLauncherResult
import com.greybox.projectmesh.components.ConnectWifiLauncherStatus
import com.greybox.projectmesh.components.meshrabiyaConnectLauncher
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState

@Composable
// We customize the viewModel since we need to inject dependencies
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { di, savedStateHandle -> HomeScreenViewModel(di, savedStateHandle) },
            defaultArgs = null)),
    deviceName: String?
)
{
    val context = LocalContext.current
    val di = localDI()
    val uiState: HomeScreenModel by viewModel.uiState.collectAsState(initial = HomeScreenModel())
    val node: VirtualNode by di.instance()
    val logger: MNetLogger by di.instance()
    val currConcurrencyKnown = viewModel.concurrencyKnown.collectAsState()
    val currConcurrencySupported = viewModel.concurrencySupported.collectAsState()

    // Request nearby wifi permission since it is necessary to start a hotspot
    val requestNearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ granted -> if (granted){
        if(context.hasNearbyWifiDevicesOrLocationPermission()){
            viewModel.onSetIncomingConnectionsEnabled(true)
        }
    } }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("BluetoothDebug", "Bluetooth permissions granted")
            viewModel.onSetIncomingConnectionsEnabled(true)
        } else {
            Log.d("BluetoothDebug", "Bluetooth permissions NOT granted")
        }
    }

    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions not needed pre-Android 12
        }
    }

    // if not known and android version >= 11, then use official api to check concurrency
    if(!currConcurrencyKnown.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        viewModel.saveConcurrencyKnown(true)
        viewModel.saveConcurrencySupported(context.hasStaApConcurrency())
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Launch the home screen
    StartHomeScreen(
        uiState = uiState,
        node = node as AndroidVirtualNode,
        onSetIncomingConnectionsEnabled = { enabled ->
            if(enabled) {
                if (!context.hasNearbyWifiDevicesOrLocationPermission()) {
                    requestNearbyWifiPermissionLauncher.launch(NEARBY_WIFI_PERMISSION_NAME)
                    return@StartHomeScreen
                }
                // check for Bluetooth permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkBluetoothPermissions()) {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE,
                        )
                    )
                    return@StartHomeScreen
                }
                if (uiState.hotspotTypeToCreate == HotspotType.WIFIDIRECT_GROUP) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    if (!wifiManager.isWifiEnabled) {
                        Toast.makeText(context, "Please Turn on Wifi", Toast.LENGTH_SHORT).show()
                        return@StartHomeScreen
                    }
                }
            }
            viewModel.onSetIncomingConnectionsEnabled(enabled)
        },
        onClickDisconnectWifiStation = viewModel::onClickDisconnectStation,
        deviceName = deviceName,
        context = context,
        currConcurrencyKnown = currConcurrencyKnown,
        currConcurrencySupported = currConcurrencySupported,
        onSetBand = viewModel::onConnectBandChanged,
        onSetHotspotTypeToCreate = viewModel::onSetHotspotTypeToCreate,
        onConnectWifiLauncherResult = { result ->
            if(result.hotspotConfig != null) {
                viewModel.onConnectWifi(result.hotspotConfig)
            }else {
                errorMessage = result.exception?.message
            }
        },
        logger = logger
    )
    // Show an error dialog when needed
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Connection Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

// Display the home screen
@Composable
fun StartHomeScreen(
    uiState: HomeScreenModel,
    node: AndroidVirtualNode,
    onSetIncomingConnectionsEnabled: (Boolean) -> Unit = { },
    onClickDisconnectWifiStation: () -> Unit = { },
    viewModel: HomeScreenViewModel = viewModel(),
    deviceName: String?,
    context: Context,
    currConcurrencyKnown: State<Boolean>,
    currConcurrencySupported: State<Boolean>,
    onSetBand: (ConnectBand) -> Unit = { },
    onSetHotspotTypeToCreate: (HotspotType) -> Unit = { },
    onConnectWifiLauncherResult: (ConnectWifiLauncherResult) -> Unit,
    logger: MNetLogger
){
    val di = localDI()
    val barcodeEncoder = remember { BarcodeEncoder() }
    var connectLauncherState by remember {
        mutableStateOf(ConnectWifiLauncherStatus.INACTIVE)
    }
    val connectLauncher = meshrabiyaConnectLauncher(
        node = node,
        logger = logger,
        onStatusChange = {
            connectLauncherState = it
        },
        onResult = onConnectWifiLauncherResult,
    )
    var userEnteredConnectUri by rememberSaveable { mutableStateOf("") }
    val showNoConcurrencyWarning by viewModel.showNoConcurrencyWarning.collectAsState()
    val showConcurrencyWarning by viewModel.showConcurrencyWarning.collectAsState()
    // connect to other device via connect uri
    fun connect(uri: String, logger: MNetLogger): Unit {
        try {
            logger(Log.INFO, "HomeScreen: Scanned link: $uri")
            // Parse the link, get the wifi connect configuration.
            val hotSpot = MeshrabiyaConnectLink.parseUri(
                uri = uri,
                json = di.direct.instance()
            ).hotspotConfig
            // if the configuration is valid, connect to the device.
            if (hotSpot != null) {
                if(hotSpot.nodeVirtualAddr !in uiState.nodesOnMesh) {
                    // Connect device thru wifi connection
                    connectLauncher.launch(hotSpot)
                }else{
                    Toast.makeText(context, "Already connected to this device", Toast.LENGTH_SHORT).show()
                    logger(Log.INFO,"Already connected to this device")
                }
            } else {
                Toast.makeText(context, "Link doesn't have a connect config", Toast.LENGTH_SHORT).show()
                logger(Log.WARN, "HomeScreen: Link doesn't have a connect config")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid Link", Toast.LENGTH_SHORT).show()
            logger(Log.WARN, "HomeScreen: Invalid Link", e)
        }
    }

    // initialize the QR code scanner
    val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        // Get the contents of the QR code
        val link = result.contents
        if (link != null) {
            connect(link, logger)
        }else{
            Toast.makeText(context, "QR Code scan doesn't return a link", Toast.LENGTH_SHORT).show()
            logger(Log.INFO,"QR Code scan doesn't return a link")
        }
    }

    // Show warning popup when device does not support STA/AP concurrency
    if (showNoConcurrencyWarning) {
        NoConcurrencyWarningDialog(onDismiss = { viewModel.dismissNoConcurrencyWarning() })
    }

    // Show warning popup when device does not support STA/AP concurrency
    if (showConcurrencyWarning) {
        ConcurrencyWarningDialog(onDismiss = { viewModel.dismissConcurrencyWarning() })
    }

    // Function to check if Wi-Fi Direct is supported
    fun isWifiDirectSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
    }

// Bluetooth connect launcher
    val bluetoothLauncher = rememberBluetoothConnectLauncher { result ->
        // when this is called, it passes back a Bluetooth Device
        val device = result.device
        if (device != null) {
            // we pass this device to our client logic in the ViewModel
            viewModel.onDeviceSelected(device) { uri ->
                // Check if URI is valid before attempting connection
                if (uri.isNotBlank()) {
                    // onDeviceSelected passes back the returned URI and
                    // sends it to connect(...) to finish connecting
                    connect(uri, logger)
                } else {
                    logger(Log.ERROR, "Failed to get URI from device: ${device.address}")
                }
            }
        } else {
            logger(Log.INFO,"No Bluetooth device selected...device = null")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item (key = "device_info") {
            LongPressCopyableText(
                context,
                text = stringResource(id = R.string.device_name) + ": ",
                textCopyable = deviceName.toString(),
                textSize = 14,
                padding = 6
            )
            LongPressCopyableText(
                context,
                text = stringResource(id = R.string.ip_address) + ": ",
                textCopyable = uiState.localAddress.addressToDotNotation(),
                textSize = 14,
                padding = 6
            )
        }

        item (key = "band_option"){
            if (uiState.connectBandVisible) {
                Text(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    text = stringResource(id = R.string.band),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row (modifier = Modifier.padding(horizontal = 6.dp)){
                    uiState.bandMenu.forEach { band ->
                        FilterChip(
                            selected = uiState.band == band,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            onClick = {
                                onSetBand(band)
                            },
                            label = {
                                Text(band.toString())
                            },
                        )
                    }
                }
            }
        }

        item (key = "hotspot_type_option") {
            if(!uiState.wifiConnectionEnabled) {
                val wifiDirectSupported = isWifiDirectSupported(context)
                Text(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    text = stringResource(id = R.string.hotspot_type),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(modifier = Modifier.padding(horizontal = 6.dp)){
                    uiState.hotspotTypeMenu.forEach { hotspotType ->
                        val isDisabled = (hotspotType == HotspotType.WIFIDIRECT_GROUP && !wifiDirectSupported)
                        FilterChip(
                            enabled = !isDisabled,
                            selected = hotspotType == uiState.hotspotTypeToCreate,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            onClick = { onSetHotspotTypeToCreate(hotspotType) },
                            label = { Text(hotspotType.toString()) }
                        )
                    }
                }
            }
        }

        item (key = "start_stop_hotspot_button"){
            // Display the "Start Hotspot" button
            val stationState = uiState.wifiState?.wifiStationState
            if (!uiState.wifiConnectionEnabled) {
                Row{
                    TransparentButton(
                        onClick = { onSetIncomingConnectionsEnabled(true) },
                        modifier = Modifier.padding(4.dp),
                        text = stringResource(id = R.string.start_hotspot),
                        // If not connected to a WiFi, enable the button
                        // Else, check if the device supports WiFi STA/AP Concurrency
                        // If it does, enable the button. Otherwise, disable it
                        enabled = if(stationState == null || stationState.status == WifiStationState.Status.INACTIVE)
                            true
                        else
                            currConcurrencySupported.value
                    )
                }
            }
            // Display the "Stop Hotspot" button
            else{
                Row{
                    TransparentButton(
                        onClick = {
                            stopHotspotConfirmationDialog(context) { onConfirm ->
                                if (onConfirm) {
                                    onSetIncomingConnectionsEnabled(false)
                                }
                            }
                        },
                        modifier = Modifier.padding(4.dp),
                        text = stringResource(id = R.string.stop_hotspot),
                        enabled = true
                    )
                }
            }
        }

        item(key = "qr_code_view"){
            // Generating QR CODE
            val connectUri = uiState.connectUri
            if (connectUri != null && uiState.wifiConnectionEnabled) {
                QRCodeView(
                    connectUri,
                    barcodeEncoder,
                    uiState.wifiState?.connectConfig?.ssid,
                    uiState.wifiState?.connectConfig?.passphrase,
                    uiState.wifiState?.connectConfig?.bssid,
                    uiState.wifiState?.connectConfig?.port.toString()
                )
                // Display connectUri
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(id = R.string.instruction_start_hotspot))
                Button(onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, connectUri)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }, modifier = Modifier.padding(4.dp), enabled = true) {
                    Text(stringResource(id = R.string.share_connect_uri))
                }
            }
        }

        item(key = "connect_via_qr_code") {
            val stationState = uiState.wifiState?.wifiStationState
            // Scan the QR CODE
            // If the stationState is not null and its status is INACTIVE,
            // It will display the option to connect via a QR code scan.
            if (stationState != null){
                if (stationState.status == WifiStationState.Status.INACTIVE){
                    Text(modifier = Modifier.padding(6.dp), text = stringResource(id = R.string.wifi_station_connection), style = TextStyle(fontSize = 16.sp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row{
                        TransparentButton(onClick = {
                            qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false)
                                .setPrompt("Scan another device to join the Mesh")
                                .setBeepEnabled(true)
                            )},
                            modifier = Modifier.padding(4.dp),
                            text = stringResource(id = R.string.connect_via_qr_code_scan),
                            // If the hotspot isn't started, enable the button
                            // Else, check if the device supports WiFi STA/AP Concurrency
                            // If it does, enable the button. Otherwise, disable it
                            enabled = if(!uiState.hotspotStatus)
                                true
                            else
                                currConcurrencySupported.value
                        )
                    }
                    Text(modifier = Modifier.padding(6.dp), text = stringResource(id = R.string.instruction))
                    TextField(
                        value = userEnteredConnectUri,
                        onValueChange = {
                            userEnteredConnectUri = it
                        },
                        label = { Text(stringResource(id = R.string.prompt_enter_uri)) }
                    )
                    TransparentButton(
                        onClick = {
                            connect(userEnteredConnectUri, logger)
                        },
                        modifier = Modifier.padding(4.dp),
                        text = stringResource(id = R.string.connect_via_entering_connect_uri),
                        // If the hotspot isn't started, enable the button
                        // Else, check if the device supports WiFi STA/AP Concurrency
                        // If it does, enable the button. Otherwise, disable it
                        enabled = if (!uiState.hotspotStatus)
                            true
                        else
                            currConcurrencySupported.value
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TransparentButton(
                        onClick = {

                            // turns the flag on to say we are in discovery
                            viewModel.onConnectViaBluetooth()

                            // this starts the Companion Device Manager UI, the name being passed in
                            // doesn't do anything because the filter is turned off
                            bluetoothLauncher.launch(MeshrabiyaBluetoothState(deviceName="Test"))

                            // turns the flag off once the device is selected
                            viewModel.onBluetoothDiscoveryLaunched()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        text = stringResource(id = R.string.connect_via_bluetooth),
                        enabled = if (!uiState.hotspotStatus) true else currConcurrencySupported.value
                    )
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
        }

        // add a Hotspot status indicator
        item(key = "hotspot_status_indicator"){
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(modifier = Modifier.padding(6.dp),
                    text = stringResource(id = R.string.hotspot_status) + ": " +
                            if (uiState.hotspotStatus) stringResource(
                                id = R.string.hotspot_status_online)
                            else stringResource(id = R.string.hotspot_status_offline))
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

fun stopHotspotConfirmationDialog(context: Context, onConfirm: (Boolean) -> Unit){
    AlertDialog.Builder(context)
        .setTitle("Do you want to turn off the hotspot?")
        .setPositiveButton("Yes"){ _, _ ->
            onConfirm(true)
        }
        .setNegativeButton("No"){ _, _ ->
            onConfirm(false)
        }
        .show()
}

// Enable users to copy text by holding down the text for a long press
@Composable
fun LongPressCopyableText(context: Context,
                          text: String,
                          textCopyable: String,
                          textSize: Int,
                          padding: Int = 0){
    val clipboardManager = LocalClipboardManager.current
    BasicText(
        text = text + textCopyable,
        style = TextStyle(
            fontSize = textSize.sp,
            color = MaterialTheme.colorScheme.onBackground),
        modifier = Modifier
            .pointerInput(textCopyable) {
                detectTapGestures(
                    onLongPress = {
                        clipboardManager.setText(AnnotatedString(textCopyable))
                        Toast
                            .makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT)
                            .show()
                    })
            }
            .padding(padding.dp)
    )
}

// display the QR code
@Composable
fun QRCodeView(qrcodeUri: String, barcodeEncoder: BarcodeEncoder, ssid: String?, password: String?,
               mac: String?, port: String?) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    // Convert dp to int once and remember the value
    val qrCodeSize = remember(density, screenWidthDp) {
        with(density) { screenWidthDp.times(0.35f).roundToPx() } // Converts to Int
    }
    val qrCodeBitMap = remember(qrcodeUri) {
        barcodeEncoder.encodeBitmap(
            qrcodeUri, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize
        ).asImageBitmap()
    }
    Row (modifier = Modifier.fillMaxWidth()) {
        // QR Code left side, Device info on the right side
        Image(
            bitmap = qrCodeBitMap,
            contentDescription = "QR Code"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "SSID: $ssid")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Password: $password")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "MAC: $mac")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Port: $port")
        }
    }
}

@Composable
fun NoConcurrencyWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("STA/AP Concurrency Not Supported") },
        text = {
            Text(
                "Based on our test, we detected that your device does not support simultaneous Wi-Fi and hotspot usage (STA/AP concurrency)."
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ConcurrencyWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("STA/AP Concurrency Supported") },
        text = {
            Text(
                "Based on our test, we detected that your device support simultaneous Wi-Fi and hotspot usage (STA/AP concurrency)."
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("OK")
            }
        }
    )
}