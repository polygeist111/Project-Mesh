//package com.greybox.projectmesh.model
//
//import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
//import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState
//import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState
//
//data class HomeScreenModel(
//    val wifiState: MeshrabiyaWifiState? = null,
//    val connectUri: String? = null,
//    val localAddress: String? = null,
//    val bluetoothState: MeshrabiyaBluetoothState? = null,
//){
//    val wifiConnected: Boolean
//        get() = wifiState?.connectConfig != null
//    val bluetoothConnected: Boolean
//        get() = bluetoothState?.deviceName != null
//}
