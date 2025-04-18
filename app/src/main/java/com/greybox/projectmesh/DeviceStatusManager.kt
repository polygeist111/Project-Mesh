package com.greybox.projectmesh

import android.util.Log
import com.greybox.projectmesh.server.AppServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress

/**
 * Centralized manager for tracking online/offline status of devices.
 * This singleton provides a single source of truth that can be observed by different parts of the app.
 */

object DeviceStatusManager {
    //private mutable state flow that stores device IP address to online status mapping
    private val _deviceStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    //public read only state flow for detection
    val deviceStatusMap: StateFlow<Map<String, Boolean>> = _deviceStatusMap.asStateFlow()

    // min time between status checks
    private const val MIN_STATUS_CHECK_INTERVAL = 5000L

    //map to track when a device was last checked
    private val lastCheckedTimes = mutableMapOf<String, Long>()

    //coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    //reference to AppServer - will be set during initialization
    private var appServer: AppServer? = null

    //special test device addresses that should be handled differently
    private val specialDevices = setOf(
        "192.168.0.99",  // Online test device
        "192.168.0.98"   // Offline test device
    )

    //initialize the device status manager with necessary dependencies
    fun initialize(server: AppServer) {
        appServer = server

        // Start periodic background checks
        startPeriodicStatusChecks()
    }

    fun updateDeviceStatus(ipAddress: String, isOnline: Boolean, verified: Boolean = false) {
        //if this is a special device, handle according to its predefined status
        if (ipAddress == "192.168.0.99") { // Online test device
            _deviceStatusMap.update { current ->
                val mutable = current.toMutableMap()
                mutable[ipAddress] = true
                mutable
            }
            Log.d("DeviceStatusManager", "Updated test device status for $ipAddress: online")
            return
        } else if (ipAddress == "192.168.0.98") { // Offline test device
            _deviceStatusMap.update { current ->
                val mutable = current.toMutableMap()
                mutable[ipAddress] = false
                mutable
            }
            Log.d("DeviceStatusManager", "Updated test device status for $ipAddress: offline")
            return
        }

        //for normal devices, if the update is verified (from a trusted component), update immediately
        if (verified) {
            _deviceStatusMap.update { current ->
                val mutable = current.toMutableMap()
                mutable[ipAddress] = isOnline
                Log.d("DeviceStatusManager", "Updated verified status for $ipAddress: ${if (isOnline) "online" else "offline"}")
                mutable
            }
            //also update the last checked time
            lastCheckedTimes[ipAddress] = System.currentTimeMillis()
        } else {
            //if not verified, only change offline->online (we'll verify before changing online->offline)
            if (isOnline && (!_deviceStatusMap.value.containsKey(ipAddress) || _deviceStatusMap.value[ipAddress] == false)) {
                // Only update if we're going from offline/unknown to online
                _deviceStatusMap.update { current ->
                    val mutable = current.toMutableMap()
                    mutable[ipAddress] = true
                    Log.d("DeviceStatusManager", "Updated status for $ipAddress: online (unverified)")
                    mutable
                }

                //schedule a verification check to confirm it's really online
                verifyDeviceStatus(ipAddress)
            } else if (!isOnline) {
                //for marking devices offline, we need verification
                verifyDeviceStatus(ipAddress)
            }
        }
    }

    //check if a device is currently online
    fun isDeviceOnline(ipAddress: String): Boolean {
        // Check if it's time to verify this device again
        val lastChecked = lastCheckedTimes[ipAddress] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastChecked > MIN_STATUS_CHECK_INTERVAL &&
            _deviceStatusMap.value[ipAddress] == true) {
            // If it's been a while since we checked and device is marked online, verify
            verifyDeviceStatus(ipAddress)
        }

        return _deviceStatusMap.value[ipAddress] ?: false
    }


    //verify if a device is actually online by attempting to connect
    fun verifyDeviceStatus(ipAddress: String) {
        //skip verification for special test devices
        if (ipAddress in specialDevices) {
            return
        }

        //check if we've verified recently
        val lastChecked = lastCheckedTimes[ipAddress] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastChecked < MIN_STATUS_CHECK_INTERVAL) {
            //checked too recently, skip
            return
        }

        scope.launch {
            lastCheckedTimes[ipAddress] = System.currentTimeMillis()

            try {
                val addr = InetAddress.getByName(ipAddress)

                //first try a basic network-level check
                val isReachable = withTimeoutOrNull(3000) {
                    addr.isReachable(2000)
                } ?: false

                if (!isReachable) {
                    //device is not reachable at network level
                    _deviceStatusMap.update { current ->
                        val mutable = current.toMutableMap()
                        mutable[ipAddress] = false
                        Log.d("DeviceStatusManager", "Device $ipAddress is unreachable (network level), marking offline")
                        mutable
                    }
                    return@launch
                }

                //then try a more meaningful app-level check
                appServer?.let { server ->
                    //attempt to request user info
                    val checkResult = server.checkDeviceReachable(addr)

                    _deviceStatusMap.update { current ->
                        val mutable = current.toMutableMap()
                        mutable[ipAddress] = checkResult
                        Log.d("DeviceStatusManager", "Verified device $ipAddress status: ${if (checkResult) "online" else "offline"} (app level)")
                        mutable
                    }

                    //if device is online, update user info and conversation status
                    if (checkResult) {
                        server.requestRemoteUserInfo(addr)
                    }
                }
            } catch (e: Exception) {
                //if any exception occurs, mark device as offline
                _deviceStatusMap.update { current ->
                    val mutable = current.toMutableMap()
                    mutable[ipAddress] = false
                    Log.d("DeviceStatusManager", "Error checking device $ipAddress, marking offline: ${e.message}")
                    mutable
                }
            }
        }
    }

    //get all online devices
    fun getOnlineDevices(): List<String> {
        return _deviceStatusMap.value.filter { it.value }.keys.toList()
    }

    //clear all device statuses
    //use when restarting app or when network changes alot
    fun clearAllStatuses() {
        _deviceStatusMap.value = emptyMap()
        lastCheckedTimes.clear()
        Log.d("DeviceStatusManager", "Cleared all device statuses")
    }

    //Start periodic background checks for device status
    private fun startPeriodicStatusChecks() {
        scope.launch {
            while (true) {
                // Check all devices marked as online
                _deviceStatusMap.value.filter { it.value }.keys.forEach { ipAddress ->
                    // Skip special devices
                    if (ipAddress !in specialDevices) {
                        verifyDeviceStatus(ipAddress)
                    }
                }

                // Wait between check cycles
                delay(30000) // 30 seconds between checks
            }
        }
    }

}
