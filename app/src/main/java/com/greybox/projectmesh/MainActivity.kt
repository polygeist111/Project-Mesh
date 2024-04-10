package com.greybox.projectmesh

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.integration.android.IntentIntegrator
import com.greybox.projectmesh.ui.theme.ProjectMeshTheme
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.bssidDataStore
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.lang.Exception
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.vnet.wifi.MeshrabiyaWifiManagerAndroid
import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    val qrIntent = IntentIntegrator(this) // For qr scanner
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request nearby devices permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 2)
            }
        }

        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }

        // Initialise Meshrabiya
        //initMesh();
        //thisNode = AndroidVirtualNode(
        //    appContext = applicationContext,
        //    dataStore = applicationContext.dataStore
        //)

        // Load content
        setContent {
            PrototypePage()
        }
    }

    lateinit var thisNodeForQr: AndroidVirtualNode
    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    private fun PrototypePage()
    {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val thisNode by remember { mutableStateOf<AndroidVirtualNode>( AndroidVirtualNode(
                appContext = applicationContext,
                dataStore = applicationContext.dataStore
            ) ) }
            val nodes by thisNode.state.collectAsState(LocalNodeState())
            val wifimanager by thisNode.meshrabiyaWifiManager.state.collectAsState(
                MeshrabiyaWifiState()
            )
            var connectLink by remember { mutableStateOf("")}

            //var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${nodes.address.addressToDotNotation()}")
            if (!nodes.connectUri.isNullOrEmpty())
            {
                Text(text = "Connection state: ${nodes.wifiState}")
                Text(text = "Join URI: ${nodes.connectUri}")

                // Show QR Code
                Image(
                    painter = rememberQrBitmapPainter(
                        content = nodes.connectUri!!,
                        size = 300.dp,
                        padding = 1.dp
                    ),
                    contentDescription = null
                )


            }
            val coroutineScope = rememberCoroutineScope()
            val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) {
                result ->
                val link = result.contents
                if (link != null)
                {
                    val connectConfig = MeshrabiyaConnectLink.parseUri(link).hotspotConfig
                    if(connectConfig != null) {
                        val job = coroutineScope.launch {
                            //try {
                                thisNode.connectAsStation(connectConfig)
                            //} catch (e: Exception) {
                                //Log(Log.ERROR,"Failed to connect ",e)
                            //}
                        }
                    }
                }
            }

            Button(content = {Text("Scan QR code")}, onClick = {
                // Open QR code scanner
                //thisNodeForQr = thisNode
                //qrIntent.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                //qrIntent.initiateScan()
                // Then gets called by intent reciever

                qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false).setPrompt("Scan another device to join the Mesh").setBeepEnabled(false))

            }) //thisNode.meshrabiyaWifiManager.connectToHotspot()



            val hotspot: () -> Unit = {
                coroutineScope.launch {
                    // On start hotspot button click...
                    // Stop any hotspots
                    //thisNode.disconnectWifiStation()
                    //thisNode.meshrabiyaWifiManager.deactivateHotspot()


                    thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_5GHZ)
                    // Report connect link
                    connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull().toString()
                }
            }

            Button(content = {Text("Start hotspot")}, onClick = hotspot)


            Text(text = "Other nodes:")
            //nodes.originatorMessages.entries
            //if (nodes.originatorMessages.isEmpty())
            //{
            //    Text(text = "N/A")
            //}
            //else

                nodes.originatorMessages.entries.forEach {
                    Text(  it.value.lastHopAddr.addressToDotNotation() + it.value.originatorMessage + it.value)

            }



            var chatLog by remember { mutableStateOf("") }

            Row {
                var chatMessage by remember { mutableStateOf("") }


                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it},
                    label = { Text("Message") }
                )
                Button(content = {Text("Send")}, onClick = fun() {

                    chatLog += "You: $chatMessage\n"
                    // SEND TO NETWORK HERE
                    chatMessage = ""
                })
            }

            Text(text = chatLog)
        }

    }
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")
    //lateinit var thisNode: AndroidVirtualNode


    
}