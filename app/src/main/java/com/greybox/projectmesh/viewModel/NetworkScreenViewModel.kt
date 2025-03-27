package com.greybox.projectmesh.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import org.kodein.di.DI
import com.greybox.projectmesh.server.AppServer
import com.ustadmobile.meshrabiya.ext.addressToByteArray
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState
import com.greybox.projectmesh.testing.TestDeviceEntry
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.instance
import java.net.InetAddress
import com.greybox.projectmesh.GlobalApp

data class NetworkScreenModel(
    val connectingInProgressSsid: String? = null,
    val allNodes: Map<Int, VirtualNode.LastOriginatorMessage> = emptyMap(),
)

class NetworkScreenViewModel(di:DI, savedStateHandle: SavedStateHandle): ViewModel() {
    // _uiState will be updated whenever there is a change in the UI state
    private val _uiState = MutableStateFlow(NetworkScreenModel())
    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<NetworkScreenModel> = _uiState.asStateFlow()
    // di is used to get the AndroidVirtualNode instance
    private val node: AndroidVirtualNode by di.instance()
    private val appServer: AppServer by di.instance()

    // launch a coroutine
    init {
        viewModelScope.launch {
            //create test device entry
            val testEntry = TestDeviceEntry.createTestEntry()

            node.state.collect{ nodeState ->
                // get the test device entry
                // Combine real nodes with test device
                val allNodesWithTest = nodeState.originatorMessages.toMutableMap()
                allNodesWithTest[testEntry.first] = testEntry.second

                Log.d("NetworkScreenViewModel", "Updating nodes. Count: ${allNodesWithTest.size}")
                Log.d("NetworkScreenViewModel", "Test device address: ${testEntry.first}")
                Log.d("NetworkScreenViewModel", "All nodes: ${allNodesWithTest.keys}")

                // update the UI state with the new state
                _uiState.update { prev -> prev.copy(
                    // update all nodes
                    allNodes = allNodesWithTest,
                    //allNodes = it.originatorMessages,
                    // update the ssid of the connecting station
                    connectingInProgressSsid =
                        if (nodeState.wifiState.wifiStationState.status == WifiStationState.Status.CONNECTING){
                            nodeState.wifiState.wifiStationState.config?.ssid
                        }
                        else{
                            null
                        }
                )}
            }
        }

        viewModelScope.launch {
            while (true) {
                try {
                    //get all connected users
                    val connectedUsers = GlobalApp.GlobalUserRepo.userRepository.getAllConnectedUsers()
                    for (user in connectedUsers) {
                        user.address?.let { ipStr ->
                            try {
                                val addr = InetAddress.getByName(ipStr)

                                //ping user by requesting online info to update status
                                appServer.requestRemoteUserInfo(addr)

                                Log.d("NetworkScreenViewModel", "Pinged user: ${user.name} at $ipStr")
                            } catch (e: Exception) {
                                //if ping fails, mark user as offline
                                GlobalApp.GlobalUserRepo.conversationRepository.updateUserStatus(
                                    userUuid = user.uuid,
                                    isOnline = false,
                                    userAddress = null
                                )
                                Log.d("NetworkScreenViewModel", "User ${user.name} appears to be offline")
                            }
                        }
                    }
                }catch (e: Exception) {
                    Log.e("NetworkScreenViewModel", "Error during periodic ping", e)
                }
                //wait for 30 secs before next ming
                delay(30000)
            }
        }
    }

    fun getDeviceName(wifiAddress: Int){
        viewModelScope.launch {
            val inetAddress = InetAddress.getByAddress(wifiAddress.addressToByteArray())
            appServer.sendDeviceName(inetAddress)
        }
    }

}