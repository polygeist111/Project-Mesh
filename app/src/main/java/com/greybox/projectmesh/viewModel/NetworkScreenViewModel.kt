package com.greybox.projectmesh.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.views.NetworkScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kodein.di.DI
import com.greybox.projectmesh.model.NetworkScreenModel
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.instance

class NetworkScreenViewModel(di:DI): ViewModel() {
    private val _uiState = MutableStateFlow(NetworkScreenModel())
    val uiState: Flow<NetworkScreenModel> = _uiState.asStateFlow()
    private val node: AndroidVirtualNode by di.instance()

    init {
        viewModelScope.launch {
            node.state.collect{
                _uiState.update { prev ->
                    prev.copy(
                        allNodes = it.originatorMessages,
                        connectingInProgressSsid =
                            if (it.wifiState.wifiStationState.status == WifiStationState.Status.CONNECTING){
                                it.wifiState.wifiStationState.config?.ssid
                            }else{
                                null
                            }
                    )
                }
            }
        }
    }

}