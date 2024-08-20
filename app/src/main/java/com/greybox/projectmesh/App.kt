//package com.greybox.projectmesh
//
//import android.app.Application
//import android.content.Context
//import android.os.Build
//import android.util.Log
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.core.intPreferencesKey
//import com.ustadmobile.meshrabiya.ext.asInetAddress
//import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
//import com.ustadmobile.meshrabiya.vnet.randomApipaAddr
//import kotlinx.coroutines.runBlocking
//import org.kodein.di.DI
//import org.kodein.di.DIAware
//import org.kodein.di.bind
//import org.kodein.di.instance
//import org.kodein.di.singleton
//import java.net.InetAddress
//
//class App : Application(), DIAware {
//    val ADDRESS_KEY = intPreferencesKey("virtual_node_address")
//    private val diModule = DI.Module("project_mesh") {
//        bind<InetAddress>(tag=TAG_VIRTUAL_ADDRESS) with singleton {
//            runBlocking {
//                val address = applicationContext.dataS { preferences ->
//                    preferences[ADDRESS_KEY] ?: 0
//                }.first()
//
//                if(address != 0) {
//                    address.asInetAddress()
//                }
//                else{
//                    randomApipaAddr().also {
//                        randomAddress -> applicationContext.dataStore.edit {
//                            it[ADDRESS_KEY] = randomAddress
//                        }
//                    }.asInetAddress()
//                }
//            }
//        }
//        bind<AndroidVirtualNode> with singleton {
//            AndroidVirtualNode(
//                appContext = applicationContext,
//                address = instance(tag = TAG_VIRTUAL_ADDRESS),
//                dataStore = applicationContext.dataStore
//            )
//        }
//    }
//
//    companion object {
//        const val TAG_VIRTUAL_ADDRESS = "virtual_address"
//
//    }
//
//}