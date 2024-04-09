package com.greybox.projectmesh

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {

    val qrIntent = IntentIntegrator(this) // For qr scanner
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load layout
        //setContentView(R.layout.activity_main)

        // Initialise Meshrabiya
        //initMesh();
        thisNode = AndroidVirtualNode(
            appContext = applicationContext,
            dataStore = applicationContext.dataStore
        )

        // Load content
        setContent {
            PrototypePage()
        }
    }
    @Composable
    private fun PrototypePage()
    {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${thisNode.address}")
            if (connectionState != null)
            {
                Text(text = "Connection state: ${connectionState?.wifiState}")
                Text(text = "Join URI: ${connectionState?.connectUri}")

                // Show QR Code
                Image(
                    painter = rememberQrBitmapPainter(
                        content = connectionState?.connectUri.toString(),
                        size = 300.dp,
                        padding = 1.dp
                    ),
                    contentDescription = null
                )


            }

            Button(content = {Text("Scan QR code")}, onClick = {
                // Open QR code scanner
                qrIntent.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                qrIntent.initiateScan()
                // Then gets called by intent reciever

            }) //thisNode.meshrabiyaWifiManager.connectToHotspot()

            val coroutineScope = rememberCoroutineScope()
            val hotspot: () -> Unit = {
                coroutineScope.launch {
                    // On start hotspot button click...
                    val connection = initMesh()
                    if (connection != null)
                    {
                        connectionState = connection
                    }

                }
            }

            Button(content = {Text("Start hotspot")}, onClick = hotspot)

            val nodes by thisNode.state.collectAsState(LocalNodeState())
            Text(text = "Other nodes:")
            if (nodes.originatorMessages.isEmpty())
            {
                Text(text = "N/A")
            }
            else
            {
                nodes.originatorMessages.forEach {
                    Text(  it.value.lastHopAddr.addressToDotNotation() + it.value.originatorMessage + it.value)
                }
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
    lateinit var thisNode: AndroidVirtualNode
    private suspend fun initMesh(): LocalNodeState? {
        // Enable hotspot
        thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_5GHZ)
        // Report connect link
        val connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull()

        if (connectLink == null)
        {
            Log.d("Network","failed to make hotspot")
            return null
        }
        else
        {
            Log.d("Network", "connect link: $connectLink")
            return connectLink
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val res = IntentIntegrator.parseActivityResult(resultCode, data)

        if (res?.contents != null)
        {
            // Try to connect
            val connectConfig = MeshrabiyaConnectLink.parseUri(res.contents).hotspotConfig
            if(connectConfig != null) {
                GlobalScope.launch {
                    try {
                        thisNode.connectAsStation(connectConfig)
                    } catch (e: Exception) {
                        //Log(Log.ERROR,"Failed to connect ",e)
                    }

                }
            }
        }
    }

    
}