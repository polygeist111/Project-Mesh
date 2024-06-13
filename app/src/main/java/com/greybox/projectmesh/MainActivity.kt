package com.greybox.projectmesh

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.room.Room
import coil.compose.rememberImagePainter
//import coil.compose.rememberImagePainter
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.db.dao.MessageDao
import com.greybox.projectmesh.db.dao.UserDao
import com.greybox.projectmesh.db.entities.Message
import com.greybox.projectmesh.db.entities.User
import com.greybox.projectmesh.db.entities.UserMessage
import com.greybox.projectmesh.debug.CrashHandler
import com.greybox.projectmesh.debug.CrashScreenActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedFileUri = uri
                    selectedFileName = uri.lastPathSegment ?: "Unknown file"
                    Log.d("FileSelection", "Selected file URI: $selectedFileUri")
                    Log.d("FileSelection", "Selected file name: $selectedFileName")
                }
            }

            else {
                Log.e("FileSelection", "File selection canceled or failed")
            }
        }

        // Request nearby devices permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 2)
            }
        }

        // Need fine location
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.

                    // Load content
                    setContent {
                        PrototypePage()
                    }
                } else -> {
                    // No location access granted.
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Permission needed")
                        .setMessage("Fine location access is required. Please enable it in Settings > Permissions > Location > Allow only while using the app")
                        .setPositiveButton("Exit") { _, _ ->
                            // Go to settings page
                            val intent: Intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.setData(uri)
                            startActivity(intent)
                            finishAffinity()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION))

        /*if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }*/

        // crash screen
        CrashHandler.init(applicationContext,CrashScreenActivity::class.java)

        // Initialise Meshrabiya
        //initMesh();
        //thisNode = AndroidVirtualNode(
        //    appContext = applicationContext,
        //    dataStore = applicationContext.dataStore
        //)

        // Init db
        db = Room.databaseBuilder(
            applicationContext,
            MeshDatabase::class.java,
            "project-mesh-db"
        )
            .fallbackToDestructiveMigration()  //add this line to handle migrations destructively lol - giving me a headache
            .allowMainThreadQueries()          //this should generally be avoided for production apps
            .build()
        messageDao = db.messageDao()
        userDao = db.userDao()


        // UUID
        val sharedPrefs = getSharedPreferences("project-mesh-uuid", Context.MODE_PRIVATE)

        // Read UUID, if not exists then generate one.
        if (!sharedPrefs.contains("UUID")) {
            // If it doesn't exist, add the string value
            val editor = sharedPrefs.edit()
            editor.putString("UUID", UUID.randomUUID().toString())
            editor.apply()
        }
        thisIDString = sharedPrefs.getString("UUID",null) ?: "ERROR"

        // Init self user
        if (!userDao.hasWithID(thisIDString))
        {
            userDao.initSelf( User(uuid=thisIDString,"Default name",0,0) )
        }

        // Allow networking on any port
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // The UI will be launched once location access given.
    }

    private lateinit var db: MeshDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var userDao: UserDao
    private lateinit var thisIDString: String

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_mesh_libmeshrabiya")



    @Composable
    private fun ConnectPage() {
        Text("Test - connect page")
    }

    @Composable
    private fun PrototypePage()
    {
        // Global to each sub page
        var thisNode by remember { mutableStateOf( AndroidVirtualNode(
            appContext = applicationContext,
            dataStore = applicationContext.dataStore
        ) ) }
        val nodes by thisNode.state.collectAsState(LocalNodeState())
        var connectLink by remember { mutableStateOf("")}
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // Threads

        // NAVIGATION
        /*val navController = rememberNavController()
        val navGraph = navController.createGraph(startDestination = "connect") {
            composable(route = "connect") { ConnectPage( /* ... */ ) }
        }
        NavHost(navController, navGraph)*/


        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {


            //var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${nodes.address.addressToDotNotation()}")
            Text(text = "This device UUID: ${thisIDString}")
            if (!nodes.connectUri.isNullOrEmpty())
            {
                if (nodes.wifiState.hotspotIsStarted)
                {
                    //Text(text = "Join URI: ${nodes.connectUri}")

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
                    Text("Ready to scan other devices. Generating this device's QR code...")
                }



            }
            val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) {
                    result ->
                val link = result.contents
                if (link != null)
                {
                    val connectConfig = MeshrabiyaConnectLink.parseUri(link).hotspotConfig
                    if(connectConfig != null) {
                        val job = coroutineScope.launch {
                            try {
                            thisNode.connectAsStation(connectConfig)
                            }
                            catch (e: Exception) {
                            }
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

                    // Try to create hotspot
                    try
                    {
                        thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_2GHZ, hotspotType = it)
                    }
                    catch (e: Exception)
                    {
                        
                    }



                    // Report connect link
                    connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull().toString()
                }
            }

            val startHotspot: () -> Unit = {
                coroutineScope.launch {
                    thisNode.disconnectWifiStation()
                    thisNode = AndroidVirtualNode(
                        appContext = applicationContext,
                        dataStore = applicationContext.dataStore
                    )

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

            Button(content = {Text("Restart hotspot")}, onClick = {startHotspot()})
            //Button(content = {Text("Start hotspot (Auto)")}, onClick = {hotspot(HotspotType.AUTO)})
            //Button(content = {Text("Start hotspot (Wifi direct)")}, onClick = {hotspot(HotspotType.WIFIDIRECT_GROUP)})
            //Button(content = {Text("Start hotspot (Local only)")}, onClick = {hotspot(HotspotType.LOCALONLY_HOTSPOT)})

            // Status message for mesh
            Text(text = "Other devices: ${nodes.originatorMessages.entries.count()}")

            /*Text(text = "Other nodes:")
            //nodes.originatorMessages.entries
            //if (nodes.originatorMessages.isEmpty())
            //{
            //    Text(text = "N/A")
            //}
            //else

                nodes.originatorMessages.entries.forEach {
                    Text(  it.value.lastHopAddr.addressToDotNotation() + it.value.originatorMessage + it.value + "Other IP: \n" + it.value.originatorMessage.connectConfig?.nodeVirtualAddr?.addressToDotNotation() + "\n---\n")
            }*/


            //check not sending filename/content
            Button(content = { Text("Select File") }, onClick = {
                // Use an intent to select a file
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                filePickerLauncher.launch(intent)
            })


            //check this it's not working
            Button(content = { Text("Send File") }, onClick = {
                selectedFileUri?.let { uri ->
                    coroutineScope.launch {
                        sendFile(context, uri, nodes, thisNode, selectedFileName)
                    }
                }

            })




            //var chatLog by remember { mutableStateOf("") }
            Row {
                var newName by remember { mutableStateOf("") }
                TextField(
                    value = newName,
                    onValueChange = { newName = it},
                    label = { Text("Profile name") }
                )
                Button(content = {Text("Update")}, onClick = fun() {
                    coroutineScope.launch {
                        userDao.updateName(thisIDString,newName)
                    }
                })
            }

            // Target
            var targetUserIP by remember { mutableStateOf(0)}
            var targetUserName by remember { mutableStateOf("everyone")}

            Row {
                var chatMessage by remember { mutableStateOf("") }


                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it},
                    label = { Text("Message ($targetUserName)") }
                )
                Button(content = {Text("Send")}, onClick = fun() {
                    if (targetUserIP == 0) chatMessage = "[BROADCAST] $chatMessage"
                    val newMessage = Message(content=chatMessage, dateReceived = System.currentTimeMillis(), id=0, sender=thisIDString )
                    coroutineScope.launch {
                        messageDao.addMessage(newMessage)
                    }

                    // SEND TO NETWORK HERE
                    for (originatorMessage in nodes.originatorMessages) {
                        // Check address is target
                        val address: InetAddress = originatorMessage.value.originatorMessage.connectConfig?.nodeVirtualAddr?.asInetAddress() ?: continue

                        if (targetUserIP != 0 && targetUserIP != originatorMessage.value.originatorMessage.connectConfig?.nodeVirtualAddr) continue

                        Log.d("DEBUG", "Sending '$chatMessage' to ${originatorMessage.value.lastHopAddr.addressToDotNotation()}")
                        //val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                        val clientSocket = thisNode.socketFactory.createSocket(address,1337)
                        // Send the UUID string and the message together.
                        clientSocket.getOutputStream().write(("$thisIDString$chatMessage").toByteArray(
                            Charset.defaultCharset()) )
                        //clientSocket.getOutputStream().bufferedWriter().flush()
                        clientSocket.close()
                    }
                    chatMessage = ""


                })
            }


            //Text(text = chatLog)

            // Watch db for profiles
            val users by userDao.getAllFlow().collectAsState(ArrayList<User>())

            // Display
            Text("Contacts:", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp))
            {
                users.forEach {
                    //Text("${it.uuid},${it.name},${it.address.addressToDotNotation()},${(System.currentTimeMillis() - it.lastSeen)/1000}s")
                    if (it.uuid != thisIDString) {
                        // Online / offline status
                        var online = false
                        nodes.originatorMessages.entries.forEach()
                        { msg ->
                            //if (it.value.originatorMessage.connectConfig?.nodeVirtualAddr? == it.name)
                            if (msg.value.originatorMessage.connectConfig?.nodeVirtualAddr == it.address) online =
                                true;
                        }
                        Box(modifier = Modifier.background(color = ( if (online) Color.Green else Color.LightGray) ))
                        {
                            Column {
                                Text(it.name)

                                fun formatDuration(totalSeconds: Long): String {
                                    val days = totalSeconds / 3600 / 24
                                    val hours = totalSeconds / 3600
                                    val minutes = (totalSeconds % 3600) / 60
                                    val seconds = totalSeconds % 60

                                    return when {
                                        days > 0 -> "$days day(s) $hours hour(s) ${
                                            minutes.toString().padStart(2, '0')
                                        } minute(s) ${
                                            seconds.toString().padStart(2, '0')
                                        } second(s)"

                                        hours > 0 -> "$hours hour(s) ${
                                            minutes.toString().padStart(2, '0')
                                        } minute(s) ${
                                            seconds.toString().padStart(2, '0')
                                        } second(s)"

                                        minutes > 0 -> "$minutes minute(s) ${
                                            seconds.toString().padStart(2, '0')
                                        } second(s)"

                                        else -> "$seconds second(s)"
                                    }
                                }

                                if (!online) Text("Offline - Last seen " + formatDuration((System.currentTimeMillis() - it.lastSeen) / 1000) + "s ago")
                                else Text("Online")

                                // DM button
                                if (online)
                                {
                                    Button(
                                        onClick = {
                                            targetUserName = it.name; targetUserIP = it.address
                                        },
                                    ) {
                                        Text("Select")
                                    }
                                }

                            }
                        }
                    }
                }
            }

            // DM all button
            Button(
                onClick = { targetUserName = "everyone"; targetUserIP = 0},
            ) {
                Text("Select everyone")
            }

            // Watch db for chat messages
            val messages by messageDao.getAllFlow().collectAsState(ArrayList<Message>())
            val messageUsers by userDao.messagesFromAllUsers().collectAsState(ArrayList<UserMessage>())

            // Display
            Text("Messages:", fontWeight = FontWeight.Bold)
            messageUsers.forEach {
                // nicen the date
                val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm", Locale.getDefault())
                val date = Date(it.dateReceived)
                Text("[${dateFormat.format(date)}] ${it.name}: ${it.content}")

            }

            // check this
            sentImageUri?.let { uri ->
                Image(
                    painter = rememberImagePainter(uri),
                    contentDescription = "Sent Image"
                )
            }

            Button(content = {Text("Delete message history")}, onClick = fun() {
                coroutineScope.launch {
                    messageDao.deleteAll(messages)
                }
            })

            // Broadcast profile info every 10 seconds.
            // val jsonString = Json.encodeToString(myObject)

            // TCP Networking
            // Add to chat when recieve data
            LaunchedEffect(Unit) {
                Log.d("DEBUG","Launchedeffect?")
                //Runs once (like useEffect null in React)

                val mainHandler = Handler(Looper.getMainLooper())

                mainHandler.post(object : Runnable {
                    override fun run() {

                        coroutineScope.launch {
                            // Make sure our user object is up to date.
                            var thisUser = userDao.getID(thisIDString)
                            thisUser.lastSeen = System.currentTimeMillis()
                            thisUser.address = thisNode.addressAsInt
                            userDao.update(thisUser)

                            // Send to everyone
                            for (originatorMessage in nodes.originatorMessages) {
                                try {
                                    val address: InetAddress = originatorMessage.value.originatorMessage.connectConfig?.nodeVirtualAddr?.asInetAddress() ?: continue
                                    val clientSocket =
                                        thisNode.socketFactory.createSocket(address, 1338)
                                    // Send the UUID string and the message together.
                                    clientSocket.getOutputStream().write(
                                        (Json.encodeToString(thisUser)).toByteArray(
                                            Charset.defaultCharset()
                                        )
                                    )
                                    //clientSocket.getOutputStream().bufferedWriter().flush()
                                    clientSocket.close()
                                } catch (_: Exception) {}

                            }
                        }

                        mainHandler.postDelayed(this, 1000 * 10)
                    }
                })

                // Thread for CHAT messages
                // Port: 1337
                // Receives: 36 char UUID then message content
                Thread(Runnable {
                    Log.d("DEBUG","TCP message thread started")
                    val serverSocket = ServerSocket(1337)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming chat...")

                        val msg = socket.getInputStream().readBytes().toString(
                            Charset.defaultCharset())

                        // The first 36 characters are the UUID - the rest are content.
                        val uuid = msg.substring(0, 36)
                        val content = msg.substring(36)

                        Log.d("DEBUG", "Message raw: $msg")
                        Log.d("DEBUG", "Message uuid: $uuid")
                        Log.d("DEBUG", "Message content: $content")

                        // Write into DB
                        val newMessage = Message(content=content, sender=uuid, dateReceived = System.currentTimeMillis(), id=0 )
                        coroutineScope.launch {
                            messageDao.addMessage(newMessage)
                        }

                        //chatLog += msg + '\n'

                        socket.close()
                        Log.d("DEBUG","Closed message connection")
                    }
                }).start()

                // Thread for USER messages
                // Port: 1338
                // Receives: 36 char UUID then message content
                Thread(Runnable {
                    Log.d("DEBUG","TCP profile thread started")
                    val serverSocket = ServerSocket(1338)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming user profile...")

                        val msg = socket.getInputStream().readBytes().toString(
                            Charset.defaultCharset())
                        Log.d("DEBUG","Profile JSON: $msg")
                        val user = Json.decodeFromString<User>(msg)

                        // Write into DB
                        coroutineScope.launch {
                            userDao.update(user)
                        }

                        //chatLog += msg + '\n'

                        socket.close()
                        Log.d("DEBUG","Closed profile connection")
                    }
                }).start()

                Thread(Runnable {
                    Log.d("DEBUG", "TCP file transfer thread started")
                    val serverSocket = ServerSocket(1339)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG", "Incoming file...")

                        val inputStream = socket.getInputStream()
                        val reader = BufferedReader(InputStreamReader(inputStream))

                        // Read file name (first line)
                        val fileName = reader.readLine()

                        // Read the remaining bytes (file content)
                        val fileBytes = inputStream.readBytes()

                        // Save the file
                        val file = File(context.filesDir, fileName)
                        file.writeBytes(fileBytes)

                        Log.d("DEBUG", "Received file: $fileName")

                        runOnUiThread {
                            val fileUri  = Uri.fromFile(file)
                            sentImageUri = fileUri
                            Log.d("DEBUG", "displayed file: $sentImageUri")
                        }

                        socket.close()
                        Log.d("DEBUG","Closed profile connection")
                    }
                }).start()
            }

        }
    }


    //at the bottom
    //you're out of touch, I'm out of time~
    private fun sendFile(
        context: Context,
        fileUri: Uri,
        nodes: LocalNodeState,
        thisNode: AndroidVirtualNode,
        selectedFileName: String
    ) {
        Log.d("DEBUG", "sendfile called")
        val inputStream = context.contentResolver.openInputStream(fileUri)
        val fileBytes = inputStream?.readBytes() ?: return

        //czech if image
        val isImage = context.contentResolver.getType(fileUri)?.startsWith("image/") ?: false


        //send to the entire hood
        for (originatorMessage in nodes.originatorMessages) {
            try {
                val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                Log.d("DEBUG", "Sending file to ${address.hostAddress}")
                val clientSocket = thisNode.socketFactory.createSocket(address, 1339)
                Log.d("DEBUG", "port opened $clientSocket")
                //send filename and content
                clientSocket.getOutputStream().write((selectedFileName + "\n").toByteArray(Charset.defaultCharset()))
                clientSocket.getOutputStream().write(fileBytes)
                clientSocket.close()
                Log.d("DEBUG", "File sent to ${address.hostAddress}")

                sentImageUri = fileUri

            }

            catch (e: Exception) {
                val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                Log.e("FileTransfer", "Failed to send file to ${address.hostAddress}", e)
            }
        }

    }
    private var sentImageUri by mutableStateOf<Uri?>(null)
    //lateinit var thisNode: AndroidVirtualNode




}