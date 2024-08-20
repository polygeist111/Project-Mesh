//package com.greybox.projectmesh.viewModel
//
//import android.content.Context
//import android.content.Intent
//import androidx.datastore.core.DataStore
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.preferencesDataStore
//import com.greybox.projectmesh.model.HomeScreenModel
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//
//
//class HomeScreenViewModel(node: AndroidVirtualNode):ViewModel(){
//
//    private val _uiState = MutableStateFlow(HomeScreenModel())
//    val uiState: Flow<HomeScreenModel> = _uiState.asStateFlow()
////    init {
////        viewModelScope.launch {
////            node.state.collect {
////                _uiState.update {
////                    prev -> prev.copy(
////                        wifiState = it.wifiState,
////                        connectUri = it.connectUri,
////                        localAddress = it.address.toString(),
////                        bluetoothState = it.bluetoothState
////                    )
////                }
////            }
////        }
////    }
//}