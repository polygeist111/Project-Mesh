package com.greybox.projectmesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

/**
 * Centralized manager for tracking online/offline status of devices.
 * This singleton provides a single source of truth that can be observed by different parts of the app.
 */

object DeviceStatusManager {
    //private mutable state flow that stores device IP address to online status mapping
    private val _deviceStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    //public read-only state flow that can be collected
    val deviceStatusMap: StateFlow<Map<String,Boolean>> = _deviceStatusMap.asStateFlow()

    //update device's online status
    fun updateDeviceStatus(ipAddress: String, isOnline: Boolean) {
        _deviceStatusMap.update { current ->
            val mutable = current.toMutableMap()
            mutable[ipAddress] = isOnline
            Log.d("DeviceStatusManager", "Updated status for $ipAddress: ${if (isOnline) "online" else "offline"}")
            mutable
        }
    }

    //check if a device is currently online
    fun isDeviceOnline(ipAddress: String): Boolean {
        return _deviceStatusMap.value[ipAddress] ?: false
    }

    //get all online devices
    fun getOnlineDevices(): List<String> {
        return _deviceStatusMap.value.filter { it.value }.keys.toList()
    }

    //clear all device statuses
    //use when restarting app or when network changes alot
    fun clearAllStatuses() {
        _deviceStatusMap.value = emptyMap()
        Log.d("DeviceStatusManager", "Cleared all device statuses")
    }

}
