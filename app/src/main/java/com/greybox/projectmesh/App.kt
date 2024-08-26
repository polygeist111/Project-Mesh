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
class App : Application(), DIAware {
    private val ADDRESS_KEY = intPreferencesKey("virtual_node_address")

    private val diModule = DI.Module("project_mesh") {
        bind<InetAddress>(tag=TAG_VIRTUAL_ADDRESS) with singleton {
            // fetch an IP address from the data store or generate a random one
            runBlocking {
                val address = applicationContext.dataStore.data.map { preferences ->
                    preferences[ADDRESS_KEY] ?: 0
                }.first()

                if(address != 0) {
                    address.asInetAddress()
                }
                else{
                    randomApipaAddr().also {
                        randomAddress -> applicationContext.dataStore.edit {
                            it[ADDRESS_KEY] = randomAddress
                        }
                    }.asInetAddress()
                }
            }
        }
        bind<AndroidVirtualNode>() with singleton {
            AndroidVirtualNode(
                appContext = applicationContext,
                address = instance(tag = TAG_VIRTUAL_ADDRESS),
                dataStore = applicationContext.dataStore
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