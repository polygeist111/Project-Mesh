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

    //track consecutive failures
    private val failureCountMap = mutableMapOf<String, Int>()

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
        // Skip verification for special test devices
        if (ipAddress in specialDevices) {
            return
        }

        // Check if we've verified recently
        val lastChecked = lastCheckedTimes[ipAddress] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastChecked < MIN_STATUS_CHECK_INTERVAL) {
            // Checked too recently, skip
            return
        }

        scope.launch {
            // Add a log to track verification attempts
            Log.d("DeviceStatusManager", "Verifying device status for $ipAddress")

            lastCheckedTimes[ipAddress] = System.currentTimeMillis()

            try {
                // IMPORTANT: Check if there are any recent messages from this device
                // This information is maintained in NetworkScreenViewModel
                // We can check if the IP is in the current known nodes list

                // Instead of directly accessing originatorMessages, we'll use the current
                // deviceStatusMap to see if the device was marked as online by any component
                val isCurrentlyOnline = _deviceStatusMap.value[ipAddress] ?: false

                // If the device was marked as online by any component and we're doing
                // a verification check, give it more attempts before marking offline
                val attemptsNeeded = if (isCurrentlyOnline) 3 else 1

                val addr = InetAddress.getByName(ipAddress)
                val isReachable = withTimeoutOrNull(3000) {
                    addr.isReachable(2000)
                } ?: false

                if (isReachable) {
                    // Reset failure count if reachable
                    failureCountMap.remove(ipAddress)

                    // Update status to online
                    _deviceStatusMap.update { current ->
                        val mutable = current.toMutableMap()
                        mutable[ipAddress] = true
                        Log.d("DeviceStatusManager", "Device $ipAddress is reachable, marking online")
                        mutable
                    }
                } else {
                    // Not reachable - increment failure count
                    val failures = (failureCountMap[ipAddress] ?: 0) + 1
                    failureCountMap[ipAddress] = failures

                    // Only mark as offline after multiple consecutive failures
                    if (failures >= attemptsNeeded) {
                        _deviceStatusMap.update { current ->
                            val mutable = current.toMutableMap()
                            mutable[ipAddress] = false
                            Log.d("DeviceStatusManager",
                                "Device $ipAddress is unreachable after $failures attempts, marking offline")
                            mutable
                        }
                    } else {
                        Log.d("DeviceStatusManager",
                            "Device $ipAddress is unreachable (attempt $failures/$attemptsNeeded), still considered online")
                    }
                }

                // Then try the app-level check if the device is believed to be online
                if (isReachable || (isCurrentlyOnline && failureCountMap[ipAddress] ?: 0 < attemptsNeeded)) {
                    appServer?.let { server ->
                        // Only do this check if we have the AppServer instance
                        try {
                            // Try a quick check to app server endpoints
                            val checkResult = server.checkDeviceReachable(addr)

                            if (checkResult) {
                                // Reset failure count on successful app-level check
                                failureCountMap.remove(ipAddress)

                                _deviceStatusMap.update { current ->
                                    val mutable = current.toMutableMap()
                                    mutable[ipAddress] = true
                                    Log.d("DeviceStatusManager",
                                        "App-level check for $ipAddress successful, marking online")
                                    mutable
                                }

                                // If device is online, update user info and conversation status
                                server.requestRemoteUserInfo(addr)
                            }else {
                                //do nothing
                            }
                        } catch (e: Exception) {
                            // Log but don't immediately change status based on this check
                            Log.d("DeviceStatusManager", "App-level check for $ipAddress failed: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // If any exception occurs, log but don't immediately mark as offline
                Log.d("DeviceStatusManager", "Error checking device $ipAddress: ${e.message}")

                // Increment failure count
                val failures = (failureCountMap[ipAddress] ?: 0) + 1
                failureCountMap[ipAddress] = failures

                // Mark as offline after 3 consecutive failures
                if (failures >= 3) {
                    _deviceStatusMap.update { current ->
                        val mutable = current.toMutableMap()
                        mutable[ipAddress] = false
                        Log.d("DeviceStatusManager",
                            "Device $ipAddress has $failures consecutive failures, marking offline")
                        mutable
                    }
                }
            }
        }
    }

    fun handleNetworkDisconnect(ipAddress: String) {
        Log.d("DeviceStatusManager", "Network layer reported disconnect for $ipAddress, immediately updating status")

        // Skip for special test devices
        if (ipAddress in specialDevices) {
            return
        }

        // Immediately update status to offline
        _deviceStatusMap.update { current ->
            val mutable = current.toMutableMap()
            mutable[ipAddress] = false
            mutable
        }

        // Reset failure count
        failureCountMap.remove(ipAddress)

        // Update conversations immediately
        updateConversations(ipAddress, false)
    }

    // Helper method to update conversations when device status changes
    private fun updateConversations(ipAddress: String, isOnline: Boolean) {
        scope.launch {
            try {
                // Get user by IP
                val user = GlobalApp.GlobalUserRepo.userRepository.getUserByIp(ipAddress)
                if (user != null) {
                    // Update conversation status
                    GlobalApp.GlobalUserRepo.conversationRepository.updateUserStatus(
                        userUuid = user.uuid,
                        isOnline = isOnline,
                        userAddress = if (isOnline) ipAddress else null
                    )
                    Log.d("DeviceStatusManager", "Updated conversation status for ${user.name}: online=$isOnline")
                }
            } catch (e: Exception) {
                Log.e("DeviceStatusManager", "Error updating conversation for $ipAddress", e)
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
