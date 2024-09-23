package com.greybox.projectmesh

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.randomApipaAddr
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.net.InetAddress

/*
initialize global variables and DI(dependency injection) container
why use DI?
All dependencies are defined in one place, which makes it easier to manage and test.
 */
class GlobalApp : Application(), DIAware {
    // it is an instance of Preferences.key<Int>, used to interact with "DataStore"
    private val addressKey = intPreferencesKey("virtual_node_address")
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

    }

    // DI container and its bindings are only set up when they are first needed
    override val di: DI by DI.lazy {
        import(diModule)
    }

    companion object {
        const val TAG_VIRTUAL_ADDRESS = "virtual_address"
    }
}