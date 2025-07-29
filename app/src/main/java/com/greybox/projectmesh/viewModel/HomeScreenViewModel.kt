package com.greybox.projectmesh.viewModel

import android.bluetooth.BluetoothDevice
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.bluetooth.BluetoothServer
import com.greybox.projectmesh.bluetooth.BluetoothUuids
import com.ustadmobile.meshrabiya.client.HttpOverBluetoothClient
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.ustadmobile.meshrabiya.vnet.wifi.WifiConnectConfig
import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState
import com.ustadmobile.meshrabiya.vnet.wifi.state.WifiStationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.DI
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.IOException
import org.kodein.di.instance
import rawhttp.core.RawHttp
import java.util.concurrent.TimeoutException



data class HomeScreenModel(
    val wifiState: MeshrabiyaWifiState? = null,
    val connectUri: String? = null,
    val localAddress: Int = 0,
    val bandMenu: List<ConnectBand> = listOf(ConnectBand.BAND_2GHZ),
    val band: ConnectBand = bandMenu.first(),
    val hotspotTypeMenu: List<HotspotType> = listOf(HotspotType.AUTO,
        HotspotType.WIFIDIRECT_GROUP,
        HotspotType.LOCALONLY_HOTSPOT),
    val hotspotTypeToCreate: HotspotType = hotspotTypeMenu.first(),
    val hotspotStatus: Boolean = false,
    val isWifiConnected: Boolean = false,
    val nodesOnMesh: Set<Int> = emptySet(),
    // added for Bluetooth tracking
    val bluetoothState: MeshrabiyaBluetoothState? = null,
    val bluetoothServerRunning: Boolean = false,
    val startBluetoothDiscovery: Boolean = false,
){
    val wifiConnectionEnabled: Boolean
        get() = wifiState?.connectConfig != null
    val connectBandVisible: Boolean
        get() = Build.VERSION.SDK_INT >= 29 && wifiState?.connectConfig == null
}

class HomeScreenViewModel(di: DI,
                          savedStateHandle: SavedStateHandle): ViewModel(){
    // inject SharedPreferences
    private val settingPrefs: SharedPreferences by di.instance(tag = "settings")
    // _uiState will be updated whenever there is a change in the UI state
    private val _uiState = MutableStateFlow(HomeScreenModel())
    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<HomeScreenModel> = _uiState.asStateFlow()
    // di is used to get the AndroidVirtualNode instance
    private val node: AndroidVirtualNode by di.instance()
    // Concurrency Known State
    private val _concurrencyKnown = MutableStateFlow(loadConcurrencyKnown())
    val concurrencyKnown: StateFlow<Boolean> = _concurrencyKnown
    // Concurrency Supported State
    private val _concurrencySupported = MutableStateFlow(loadConcurrencySupported())
    val concurrencySupported: StateFlow<Boolean> = _concurrencySupported

    // inject Bluetooth server instance
    private val bluetoothServer: BluetoothServer by di.instance()
    // inject Bluetooth client instance
    private val bluetoothClient: HttpOverBluetoothClient by di.instance()
    private val rawHttp: RawHttp by di.instance()

    private val sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == CONCURRENCY_KNOWN_KEY || key == CONCURRENCY_SUPPORTED_KEY) {
            _concurrencyKnown.value = loadConcurrencyKnown()
            _concurrencySupported.value = loadConcurrencySupported()
        }
    }

    // control the visibility of the STA/AP concurrency doesn't support warning popup
    private val _showNoConcurrencyWarning = MutableStateFlow(false)
    val showNoConcurrencyWarning: StateFlow<Boolean> = _showNoConcurrencyWarning.asStateFlow()

    // control the visibility of the STA/AP concurrency support warning popup
    private val _showConcurrencyWarning = MutableStateFlow(false)
    val showConcurrencyWarning: StateFlow<Boolean> = _showConcurrencyWarning.asStateFlow()

    init {
        // launch a coroutine
        viewModelScope.launch {
            // collect the state flow of the AndroidVirtualNode
            node.state.collect {
                // update the UI state with the new state
                _uiState.update { prev ->
                    // Creates a new instance of the state,
                    // copying the existing properties and updating only the ones specified.
                    prev.copy(
                        wifiState = it.wifiState,
                        connectUri = it.connectUri,
                        localAddress = it.address,
                        hotspotStatus = it.wifiState.hotspotIsStarted,
                        nodesOnMesh = it.originatorMessages.keys
                    )
                }
            }
        }
        if (node.meshrabiyaWifiManager.is5GhzSupported){
            _uiState.update { prev ->
                prev.copy(
                    bandMenu = listOf(ConnectBand.BAND_5GHZ, ConnectBand.BAND_2GHZ),
                    band = ConnectBand.BAND_5GHZ
                )
            }
        }
        settingPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        bluetoothWatcher()
    }

    private fun loadConcurrencyKnown(): Boolean {
        return settingPrefs.getBoolean(CONCURRENCY_KNOWN_KEY, false)
    }

    fun saveConcurrencyKnown(concurrencyKnown: Boolean) {
        _concurrencyKnown.value = concurrencyKnown
        settingPrefs.edit().putBoolean(CONCURRENCY_KNOWN_KEY, concurrencyKnown).apply()
    }

    // -------- Bluetooth Server Functions Start ---------
    private fun updateBluetoothServerURI(connectURI: String) {
        try{
            Log.d("BluetoothDebug", "Received new URI: $connectURI")
            bluetoothServer.updateConnectURI(connectURI)

            val currentState = _uiState.value

            if (currentState.wifiConnectionEnabled && bluetoothServer.hasValidURI()) {
                Log.d("BluetoothDebug", "URI update complete and hotspot enabled. Starting Bluetooth Server")
                startBluetoothServer()
            } else {
                Log.d("BluetoothDebug", "URI update complete but hotspot not enabled.")
            }

        } catch (e: Exception) {
            Log.e("BluetoothError", "Failed to update Bluetooth Server URI", e)
        }
    }

    private fun startBluetoothServer() {
        try {
            val currentState = _uiState.value

            if(!currentState.wifiConnectionEnabled){
                Log.d("BluetoothDebug", "Failed to start server, hotspot disabled.")
                return
            }

            if (!bluetoothServer.hasValidURI()){
                Log.d("BluetoothDebug", "Failed to start server, URI is not valid")
                return
            }

            if (currentState.bluetoothServerRunning){
                Log.d("BluetoothDebug", "Failed to start server, the server is already running")
                return
            }

            bluetoothServer.start()
            _uiState.update { it.copy(bluetoothServerRunning = true) }
            Log.d("BluetoothDebug", "Bluetooth Server started successful")

        } catch (e: Exception) {
            Log.e("BluetoothError", "Failed to start Bluetooth server", e)
        }
    }

    private fun stopBluetoothServer() {
        try{
            val currentState = _uiState.value

            if(!currentState.bluetoothServerRunning){
                Log.d("BluetoothDebug", "Bluetooth Server not running, nothing to stop")
                return
            }

            bluetoothServer.stop()
            _uiState.update { it.copy(bluetoothServerRunning = false) }
            Log.d("BluetoothDebug", "Bluetooth server successfully stopped")

        } catch (e: Exception) {
            Log.e("BluetoothError", "Failed to stop Bluetooth Server", e)
        }
    }

    private fun bluetoothWatcher() {
        viewModelScope.launch {
            uiState
                .map { it.connectUri }
                .distinctUntilChanged()
                .collect { connectUri ->
                    if (connectUri != null) {
                        Log.d("BluetoothDebug", "This node's URI has been generated $connectUri")

                        // make a call to update the server if the hotspot is enabled
                        if(_uiState.value.wifiConnectionEnabled) {
                            updateBluetoothServerURI(connectUri)
                        } else {
                            Log.d("BluetoothDebug", "URI is available but the hotspot is disabled")
                        }
                    }
                }
        }

        viewModelScope.launch {
            uiState
                .map { it.wifiConnectionEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    Log.d("BluetoothDebug", "wifiConnectionEnabled state changed: $enabled")

                    if (enabled) {
                        Log.d("BluetoothDebug", "Hotspot enabled, Bluetooth server starting...")
                        startBluetoothServer()
                    } else {
                        Log.d("BluetoothDebug", "Hotspot disabled, Bluetooth server stopping...")
                        stopBluetoothServer()
                    }
                }
        }
    }
// ---------------- Bluetooth Server Functions End ------

// ---------------- Bluetooth Client Functions Start ----
fun onDeviceSelected(
    device: BluetoothDevice?,
    onUriReceived: (String) -> Unit
) {
    _uiState.update {
        it.copy(startBluetoothDiscovery = false)
    }

    if (device == null) {
        Log.d("BluetoothDebug", "Bluetooth device = null!!")
        return
    }

    viewModelScope.launch {
        try {
            Log.d("BluetoothDebug", "Connecting to device: ${device.address}")

            // we build the request to get the URI
            val request = rawHttp.parseRequest(
                "GET /api/connect-uri HTTP/1.1\r\n" +
                        "Host: ${device.address.replace(":", "-")}.bluetooth\r\n" +
                        "User-Agent: Meshrabiya\r\n" +
                        "\r\n"
            )

            val response = bluetoothClient.sendRequest(
                remoteAddress = device.address,
                uuidMask = BluetoothUuids.ALLOCATION_SERVICE_UUID,
                request = request
            )

            response.use {btResponse ->
                when (btResponse.response.statusCode) {
                    200 -> {
                        Log.d("BluetoothDebug", "200 -- OKAY!")

                        val bodyBytes = btResponse.response.body.get().asRawBytes()
                        val uri = String(bodyBytes).trim()

                        Log.d("BluetoothDebug", "Received URI: $uri")
                        onUriReceived(uri)
                    }
                    503 -> {
                        Log.d("BluetoothDebug", "Server busy")
                    }
                    else -> {
                        Log.e("BluetoothError", "Unexpected response: ${btResponse.response.statusCode}")
                    }
                }
            }


        } catch(e : Exception){
            Log.e("BluetoothError", "Unexpected Error", e)
        } catch(e: TimeoutException){
            Log.e("BluetoothError", "Timeout!", e)
        } catch(e: IOException){
            Log.e("BluetoothError", "Connection failed!", e)
        }
    }
}



fun onConnectViaBluetooth() {
    Log.d("BluetoothDebug", "ViewModel: onConnectViaBluetooth called!")

    _uiState.update {
        Log.d("BluetoothDebug", "Updating startBluetoothDiscovery = true")
        it.copy(startBluetoothDiscovery = true)
    }
}

    fun onBluetoothDiscoveryLaunched() {
        Log.d("BluetoothDebug", "startBluetoothDiscovery = false")
        _uiState.update {
            it.copy(startBluetoothDiscovery = false)
        }
    }

// ---------------- Bluetooth Client Functions End ------
    private fun loadConcurrencySupported(): Boolean {
        return settingPrefs.getBoolean(CONCURRENCY_SUPPORTED_KEY, true)
    }

    fun saveConcurrencySupported(concurrencySupported: Boolean) {
        _concurrencySupported.value = concurrencySupported
        settingPrefs.edit().putBoolean(CONCURRENCY_SUPPORTED_KEY, concurrencySupported).apply()
    }

    fun onConnectBandChanged(band: ConnectBand) {
        _uiState.update { prev ->
            prev.copy(
                band = band
            )
        }
    }

    fun onSetHotspotTypeToCreate(hotspotType: HotspotType) {
        _uiState.update { prev ->
            prev.copy(
                hotspotTypeToCreate = hotspotType
            )
        }
    }

    fun onSetIncomingConnectionsEnabled(enable: Boolean) {
        viewModelScope.launch {
            val wasWifiConnected = _uiState.value.wifiState?.wifiStationState?.status
            try {
                Log.d("HotspotDebug", "Attempt 1: Setting Hotspot to $enable")
                val response = withTimeoutOrNull(1500) {
                    node.setWifiHotspotEnabled(
                        enabled = enable,
                        preferredBand = _uiState.value.band,
                        hotspotType = _uiState.value.hotspotTypeToCreate
                    )
                }
                if (response == null) {
                    Log.w("HotspotDebug", "No response within 1500ms, Retrying...")
                    // Retry once
                    val retryResponse = node.setWifiHotspotEnabled(
                        enabled = enable,
                        preferredBand = _uiState.value.band,
                        hotspotType = _uiState.value.hotspotTypeToCreate
                    )
                    Log.d("HotspotDebug", "Retry successful: $retryResponse")
                }
                else {
                    Log.d("HotspotDebug", "Hotspot set successfully: $response")
                }
                if (!_concurrencyKnown.value && Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                    delay(500)
                    val isWifiStillConnected = _uiState.value.wifiState?.wifiStationState?.status
                    if (wasWifiConnected == WifiStationState.Status.AVAILABLE)
                    {
                        if(isWifiStillConnected == WifiStationState.Status.INACTIVE || isWifiStillConnected == null){
                            markStaApConcurrencyUnsupported()
                            Log.d("HotspotDebug", "Wi-Fi disconnected after enabling hotspot. STA/AP concurrency NOT supported.")
                        }
                        else{
                            markStaApConcurrencySupported()
                            Log.d("HotspotDebug", "Wi-Fi still connected after enabling hotspot. STA/AP concurrency supported.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HotspotDebug", "Failed to set hotspot: ${e.message}")
            }
        }
    }

    // This function is responsible for connecting to a wifi network as a station (Client Mode)
    fun onConnectWifi(
        hotSpotConfig: WifiConnectConfig
    ){
        viewModelScope.launch {
            try{
                val wasHotspotOnline = _uiState.value.hotspotStatus
                node.connectAsStation(hotSpotConfig)
                if(!_concurrencyKnown.value && Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                    delay(500)
                    val isHotspotStillOnline = _uiState.value.hotspotStatus
                    if(wasHotspotOnline){
                        if(!isHotspotStillOnline){
                            markStaApConcurrencyUnsupported()
                        }
                        else{
                            markStaApConcurrencySupported()
                        }
                    }
                }
            }
            catch (e: Exception){
                Log.e("HomeScreenViewModel", "onConnectWifi: ${e.message}")
            }
        }
    }

    // disconnect the wifi station
    fun onClickDisconnectStation(){
        viewModelScope.launch {
            node.disconnectWifiStation()
        }
    }

    private fun markStaApConcurrencyUnsupported(){
        saveConcurrencyKnown(true)
        saveConcurrencySupported(false)
        _showNoConcurrencyWarning.value = true
    }

    private fun markStaApConcurrencySupported(){
        saveConcurrencyKnown(true)
        saveConcurrencySupported(true)
        _showConcurrencyWarning.value = true
    }

    fun dismissNoConcurrencyWarning() {
        _showNoConcurrencyWarning.value = false
    }

    fun dismissConcurrencyWarning() {
        _showConcurrencyWarning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the listener when ViewModel is cleared
        settingPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    companion object{
        private const val CONCURRENCY_KNOWN_KEY = "concurrency_known"
        private const val CONCURRENCY_SUPPORTED_KEY = "concurrency_supported"
    }
}