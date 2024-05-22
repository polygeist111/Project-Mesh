package com.greybox.projectmesh

import android.content.ContentQueryMap
import android.content.Context
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.zxing.integration.android.IntentIntegrator
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.db.dao.MessageDao
import com.greybox.projectmesh.db.entities.Message
import com.greybox.projectmesh.debug.CrashHandler
import com.greybox.projectmesh.debug.CrashScreenActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.io.IOException
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import java.time.Duration
import java.util.Scanner
import java.util.Date


class MainActivity : ComponentActivity() {
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

        // crash screen
        CrashHandler.init(applicationContext,CrashScreenActivity::class.java)

        // Initialise Meshrabiya
        //initMesh();
        //thisNode = AndroidVirtualNode(
        //    appContext = applicationContext,
        //    dataStore = applicationContext.dataStore
        //)

        // Init db
        db = Room.databaseBuilder(applicationContext,MeshDatabase::class.java,"project-mesh-db").build()
        messageDao = db.messageDao()

        // Load content
        setContent {
            PrototypePage()
        }

        // Allow networking on any port
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private lateinit var db: MeshDatabase
    private lateinit var messageDao: MessageDao

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_mesh_libmeshrabiya")

    @Composable
    private fun PrototypePage()
    {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            var thisNode by remember { mutableStateOf( AndroidVirtualNode(
                appContext = applicationContext,
                dataStore = applicationContext.dataStore
            ) ) }
            val nodes by thisNode.state.collectAsState(LocalNodeState())
            var connectLink by remember { mutableStateOf("")}

            //var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${nodes.address.addressToDotNotation()}")
            if (!nodes.connectUri.isNullOrEmpty())
            {
                Text(text = "Connection state: ${nodes.wifiState}")

                if (nodes.wifiState.hotspotIsStarted)
                {
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
                else
                {
                    Text("Start a hotspot to show Connect Link and QR code")
                }



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

            // Crash button for testing the crash handler
            //Button(content = {Text("die")}, onClick = { throw Error("Crash and burn") } )

            Button(content = {Text("Scan QR code")}, onClick = {
                // Open QR code scanner
                //thisNodeForQr = thisNode
                //qrIntent.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                //qrIntent.initiateScan()
                // Then gets called by intent reciever

                qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false).setPrompt("Scan another device to join the Mesh").setBeepEnabled(false))

            }) //thisNode.meshrabiyaWifiManager.connectToHotspot()

            val hotspot: (type: HotspotType) -> Unit = {
                coroutineScope.launch {
                    // On start hotspot button click...
                    // Stop any hotspots
                    //thisNode.disconnectWifiStation()
                    //thisNode.meshrabiyaWifiManager.deactivateHotspot()

                    // Try 5GHz
                    try
                    {
                        thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_5GHZ, hotspotType = it)
                    }
                    catch (e: Exception)
                    {
                        // Try 2.5GHz
                        try {
                            thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand.BAND_2GHZ, hotspotType = it)
                        }
                        catch (e: Exception)
                        {

                        }
                    }



                    // Report connect link
                    connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull().toString()
                }
            }

            val startHotspot: () -> Unit = {
                coroutineScope.launch {
                    // Try AUTO
                    hotspot(HotspotType.AUTO)

                    // Wait 5 sec...
                    delay(5000)

                    if (!nodes.wifiState.hotspotIsStarted)
                    {
                        // Recreate node
                        thisNode.disconnectWifiStation()
                        thisNode = AndroidVirtualNode(
                            appContext = applicationContext,
                            dataStore = applicationContext.dataStore
                        )
                        hotspot(HotspotType.LOCALONLY_HOTSPOT)
                    }
                }
            }

            LaunchedEffect(Unit) {
                startHotspot()
            }

            Button(content = {Text("Start hotspot (Auto)")}, onClick = {hotspot(HotspotType.AUTO)})
            Button(content = {Text("Start hotspot (Wifi direct)")}, onClick = {hotspot(HotspotType.WIFIDIRECT_GROUP)})
            Button(content = {Text("Start hotspot (Local only)")}, onClick = {hotspot(HotspotType.LOCALONLY_HOTSPOT)})

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



            //var chatLog by remember { mutableStateOf("") }

            Row {
                var chatMessage by remember { mutableStateOf("") }


                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it},
                    label = { Text("Message") }
                )
                Button(content = {Text("Send")}, onClick = fun() {
                    val newMessage = Message(content="You: $chatMessage\n", sender=0, dateReceived = System.currentTimeMillis(), id=0 )
                    coroutineScope.launch {
                        messageDao.addMessage(newMessage)
                    }

                    // SEND TO NETWORK HERE
                    for (originatorMessage in nodes.originatorMessages) {
                        Log.d("DEBUG", "Sending '$chatMessage' to ${originatorMessage.value.lastHopAddr.addressToDotNotation()}")
                        val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                        val clientSocket = thisNode.socketFactory.createSocket(address,1337)
                        clientSocket.getOutputStream().write(("(${originatorMessage.value.lastHopAddr.addressToDotNotation()}) $chatMessage\n").toByteArray(
                            Charset.defaultCharset()) )
                        //clientSocket.getOutputStream().bufferedWriter().flush()
                        clientSocket.close()
                    }
                    chatMessage = ""
                })
            }

            //Text(text = chatLog)

            // Watch db for chat messages
            val messages by messageDao.getAllFlow().collectAsState(ArrayList<Message>())

            // Display
            for (m in messages)
            {
                Text("(${m.sender.addressToDotNotation()}) ${m.content}")
            }

            Button(content = {Text("Delete message history")}, onClick = fun() {
              coroutineScope.launch {
                  messageDao.deleteAll(messages)
              }
            })

            // TCP Networking
            // Add to chat when recieve data
            LaunchedEffect(Unit) {
                Log.d("DEBUG","Launchedeffect?")
                //Runs once (like useEffect null in React)
                // Thread that listens for TCP data on port 1337, prints to chat
                Thread(Runnable {
                    Log.d("DEBUG","TCP thread started")
                    val serverSocket = ServerSocket(1337)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming chat...")

                        val msg = socket.getInputStream().readBytes().toString(
                            Charset.defaultCharset())
                        Log.d("DEBUG", "Message info: $msg")

                        // Write into DB
                        val newMessage = Message(content=msg, sender=socket.localAddress.requireAddressAsInt(), dateReceived = System.currentTimeMillis(), id=0 )
                        coroutineScope.launch {
                            messageDao.addMessage(newMessage)
                        }

                        //chatLog += msg + '\n'

                        socket.close()
                        Log.d("DEBUG","Closed connection")
                    }
                }).start()
            }

        }
    }

    //lateinit var thisNode: AndroidVirtualNode


    
}