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
import androidx.activity.viewModels
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
import com.greybox.projectmesh.components.BottomNavApp
//import com.greybox.projectmesh.viewModel.HomeScreenViewModel

class MainActivity : ComponentActivity() {
    // private val mainViewModel: MainViewModel by viewModels()
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    // private val homeScreenViewModel: HomeScreenViewModel by viewModels()
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
        // Need fine location
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.

                    // Load content
                    setContent {
                        BottomNavApp()
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

}
