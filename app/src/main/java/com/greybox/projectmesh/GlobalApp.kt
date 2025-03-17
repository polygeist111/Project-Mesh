package com.greybox.projectmesh

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.messaging.repository.ConversationRepository
import com.greybox.projectmesh.server.AppServer
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.randomApipaAddr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.io.File
import java.net.InetAddress
import java.time.Duration
import com.greybox.projectmesh.user.UserRepository
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.testing.TestDeviceService
import com.greybox.projectmesh.user.UserEntity
/*
initialize global variables and DI(dependency injection) container
why use DI?
All dependencies are defined in one place, which makes it easier to manage and test.
 */
class GlobalApp : Application(), DIAware {
    // it is an instance of Preferences.key<Int>, used to interact with "DataStore"
    private val addressKey = intPreferencesKey("virtual_node_address")
    /*data object DeviceInfoManager {
        // Global HashMap to store IP-DeviceName mapping
        val deviceNameMap = HashMap<String, String?>()

        // Helper method to add/update a device name
        fun addDevice(ipAddress: String, name: String?) {
            deviceNameMap[ipAddress] = name
        }

        fun removeDevice(ipAddress: String) {
            deviceNameMap.remove(ipAddress)
        }

        fun getDeviceName(inetAddress: String): String? {
            return deviceNameMap[inetAddress]
        }

        fun getDeviceName(inetAddress: InetAddress): String? {
            return deviceNameMap[inetAddress.hostAddress]
        }

        fun getChatName(inetAddress: InetAddress): String {
            return inetAddress.hostAddress
//            val deviceName = getDeviceName(inetAddress) ?: "Unknown"
//            val addressDotNotation = inetAddress.hostAddress
//            return "$deviceName ($addressDotNotation)"
        }
    }*/
    object GlobalUserRepo {
        // Lateinit or lazy property
        lateinit var userRepository: UserRepository
        lateinit var prefs: SharedPreferences
        lateinit var conversationRepository: ConversationRepository
    }
    override fun onCreate() {
        super.onCreate()
        // Once you have the DI container, retrieve the userRepository instance:
        val repo: UserRepository by di.instance()
        GlobalUserRepo.userRepository = repo
        val settingPref: SharedPreferences by di.instance(tag = "settings")
        GlobalUserRepo.prefs = settingPref
        val convRepo: ConversationRepository by di.instance()
        GlobalUserRepo.conversationRepository = convRepo

        //add test convos
        insertTestConversations()
    }

    fun insertTestConversations() {
        GlobalScope.launch {
            try {
                //get database instance
                val db: MeshDatabase by di.instance()

                //check if test convos aready exist
                val currentConversations = GlobalUserRepo.conversationRepository.getAllConversations().first()
                if (currentConversations.isNotEmpty()){
                    Log.d("GlobalApp", "Test conversations already exist, skipping insertion")
                    return@launch
                }

                val localUuid = GlobalUserRepo.prefs.getString("UUID", null) ?: "local-user"

                //insert test convo with test device
                val testDevice = TestDeviceService.getTestDeviceAddress()
                val testUser = UserEntity(
                    uuid = "test-device-uuid",
                    name = TestDeviceService.TEST_DEVICE_NAME,
                    address = testDevice.hostAddress
                )

                //make sure the test user exists in the database
                GlobalUserRepo.userRepository.insertOrUpdateUser(
                    testUser.uuid,
                    testUser.name,
                    testUser.address
                )

                //create convo with the test device
                val onlineConversation = GlobalUserRepo.conversationRepository.getOrCreateConversation(
                    localUuid = localUuid,
                    remoteUser = testUser
                )

                //Create online test message
                val onlineTestMessage = Message(
                    id = 0,
                    dateReceived = System.currentTimeMillis(),
                    content = "hello world! this is a test message.",
                    sender = TestDeviceService.TEST_DEVICE_NAME,
                    chat = TestDeviceService.TEST_DEVICE_NAME  // THIS IS THE KEY! The chat name must match
                )

                db.messageDao().addMessage(onlineTestMessage)

                //update convo with a test message
                GlobalUserRepo.conversationRepository.updateWithMessage(
                    conversationId = onlineConversation.id,
                    message = onlineTestMessage
                )

                //create offline test conversation
                val offlineUser = UserEntity(
                    uuid = "offline-test-device-uuid",
                    name = TestDeviceService.TEST_DEVICE_NAME_OFFLINE,
                    address = null // null address means offline
                )

                //make sure the offine test user exists in the database
                GlobalUserRepo.userRepository.insertOrUpdateUser(
                    offlineUser.uuid,
                    offlineUser.name,
                    offlineUser.address
                )

                //Create convo with the offline test device
                val offlineConversation = GlobalUserRepo.conversationRepository.getOrCreateConversation(
                    localUuid = localUuid,
                    remoteUser = offlineUser
                )

                //create offline test message
                val offlineTestMessage = Message(
                    id = 0,
                    dateReceived = System.currentTimeMillis() - 3600000, // 1 hour ago
                    content = "I'm currently offline. Messages won't be delivered.",
                    sender = TestDeviceService.TEST_DEVICE_NAME_OFFLINE,
                    chat = TestDeviceService.TEST_DEVICE_NAME_OFFLINE  // This chat name must match
                )

                //save to message database directly
                db.messageDao().addMessage(offlineTestMessage)

                GlobalUserRepo.conversationRepository.updateWithMessage(
                    conversationId = offlineConversation.id,
                    message = offlineTestMessage
                )

                Log.d("GlobalApp", "Test conversations inserted successfully")
            }catch (e: Exception ) {
                Log.e("GlobalApp", "Error inserting test conversation", e)
            }
        }
    }
    private val diModule = DI.Module("project_mesh") {
        // create a single instance of "InetAddress" for the entire lifetime of the application
        bind<InetAddress>(tag=TAG_VIRTUAL_ADDRESS) with singleton {
            // fetch an IP address from the data store or generate a random one
            // Run a coroutine in a blocking way, it will block the main thread
            runBlocking {
                // fetch the address from the data store
                val address = applicationContext.networkDataStore.data.map { preference ->
                    preference[addressKey] ?: 0
                }.first()

                // if the address is not 0, converted to an IP address
                if(address != 0) {
                    address.asInetAddress()
                }
                else{
                    // if not, generate a random one,
                    // store it in the data store and converted to IP address
                    randomApipaAddr().also {
                            randomAddress -> applicationContext.networkDataStore.edit {
                        // "it" used to access the 'Preferences' object
                        it[addressKey] = randomAddress
                    }
                    }.asInetAddress()
                }
            }
        }
        bind <Json>() with singleton {
            Json {
                encodeDefaults = true
            }
        }
        /*
        Ensuring a directory named "www" was created
        */
        bind<File>(tag = TAG_WWW_DIR) with singleton {
            File(filesDir, "www").also{
                if(!it.exists()) {
                    it.mkdirs()
                }
            }
        }

        bind<File>(tag = TAG_RECEIVE_DIR) with singleton {
            File(filesDir, "receive")
        }

        bind<AndroidVirtualNode>() with singleton {
            // initialize the AndroidVirtualNode Constructor
            AndroidVirtualNode(
                appContext = applicationContext,
                json = instance(),
                // inject the "InetAddress" instance
                address = instance(tag = TAG_VIRTUAL_ADDRESS),
                dataStore = applicationContext.networkDataStore
            )
        }
        // The OkHttpClient will be created only once and shared across the app when needed
        bind<OkHttpClient>() with singleton {
            val node: AndroidVirtualNode = instance()
            OkHttpClient.Builder()
                .socketFactory(node.socketFactory)
                // The maximum time to wait for a connection to be established
                .connectTimeout(Duration.ofSeconds(30))
                // The maximum time to wait for data to be read from the server
                .readTimeout(Duration.ofSeconds(30))
                // The maximum time to wait for data to be written to the server
                .writeTimeout(Duration.ofSeconds(30))
                .build()
        }

        bind<MeshDatabase>() with singleton {
            Room.databaseBuilder(applicationContext,
                MeshDatabase::class.java,
                "mesh-database"
            )
                .addMigrations(object : Migration(3,4){
                    override fun migrate(database: SupportSQLiteDatabase){

                        //create convo table
                        database.execSQL(
                            """
                                CREATE TABLE IF NOT EXISTS conversations ( 
                                    id TEXT PRIMARY KEY NOT NULL, 
                                    user_uuid TEXT NOT NULL, 
                                    user_name TEXT NOT NULL, 
                                    user_address TEXT, 
                                    last_message TEXT, 
                                    last_message_time INTEGER NOT NULL, 
                                    unread_count INTEGER NOT NULL DEFAULT 0, 
                                    is_online INTEGER NOT NULL DEFAULT 0
                            )
                            """
                        )
                    }
                })
                .fallbackToDestructiveMigration() // handle migrations destructively
//                .allowMainThreadQueries() // this should generally be avoided for production apps
                .build()
        }

        bind<SharedPreferences>(tag = "settings") with singleton {
            applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        }
        bind<UserRepository>() with singleton {
            UserRepository(instance<MeshDatabase>().userDao())
        }

        bind<ConversationRepository>() with singleton {
            ConversationRepository(instance<MeshDatabase>().conversationDao(), di)
        }

        bind<AppServer>() with singleton {
            val node: AndroidVirtualNode = instance()
            AppServer(
                appContext = applicationContext,
                httpClient = instance(),
                port = AppServer.DEFAULT_PORT,
                name = node.addressAsInt.addressToDotNotation(),
                localVirtualAddr = node.address,
                receiveDir = instance(tag = TAG_RECEIVE_DIR),
                json = instance(),
                di = di,
                db = instance(),
                userRepository = instance()
            )
        }

        onReady {
            // clears all data in the existing tables
            GlobalScope.launch {
                instance<MeshDatabase>().messageDao().clearTable()
            }
            instance<AppServer>().start()
            Log.d("AppServer", "Server started successfully on port: ${AppServer.DEFAULT_PORT}")
        }
    }

    // DI container and its bindings are only set up when they are first needed
    override val di: DI by DI.lazy {
        import(diModule)
    }

    companion object {
        const val TAG_VIRTUAL_ADDRESS = "virtual_address"
        const val TAG_RECEIVE_DIR = "receive_dir"
        const val TAG_WWW_DIR = "www_dir"
    }
}