package com.greybox.projectmesh.extension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ustadmobile.meshrabiya.MeshrabiyaConstants

/*
context is a class that provides access to application-specific resources and classes.
This File contains several context related extension functions that will use in this app.
 */

/**
 * On Android 13+ we can use the NEARBY_WIFI_DEVICES permission instead of the location permission.
 * On earlier versions, we need fine location permission
 */
val NEARBY_WIFI_PERMISSION_NAME = if(Build.VERSION.SDK_INT >= 33){
    Manifest.permission.NEARBY_WIFI_DEVICES
}else {
    Manifest.permission.ACCESS_FINE_LOCATION
}

// check if the app has the nearby wifi devices permission
fun Context.hasNearbyWifiDevicesOrLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this, NEARBY_WIFI_PERMISSION_NAME
    ) == PackageManager.PERMISSION_GRANTED
}

// check if the app has the bluetooth connect permission
fun Context.hasBluetoothConnectPermission(): Boolean {
    return if(Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }else {
        true
    }
}

// check if the app has scan permission (or if it needs it at all)
fun Context.hasBluetoothScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
    } else {
        // On Android 6–11, scanning requires location permission
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}

// check for advertise permission (or if it needs it)
fun Context.hasBluetoothAdvertisePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
    } else true
}

// this lets us ask for the exact permissions that we need
// when we know what's needed for each version, we can cut this list down
// to only what we exactly need.
fun Context.requiredBtPermissions(needsScan: Boolean, needsAdvertise: Boolean): Array<String> {
    return if (Build.VERSION.SDK_INT >= 31) {
        buildList {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            if (needsScan) add(Manifest.permission.BLUETOOTH_SCAN)
            if (needsAdvertise) add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }.toTypedArray()
    } else {
        // Classic BT connect is install-time; for scan we must request location
        if (needsScan) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) else emptyArray()
    }
}
// make sure we have all the Bluetooth permissions we need
fun Context.hasAll(perms: Array<String>): Boolean =
    perms.all { p -> ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED }

// create a DataStore instance that Meshrabiya can use to remember networks
val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")

// Check if the device supports WiFi STA/AP Concurrency
fun Context.hasStaApConcurrency(): Boolean {
    return Build.VERSION.SDK_INT >= 30 && getSystemService(WifiManager::class.java).isStaApConcurrencySupported
}

fun Context.deviceInfo(): String {
    val wifiManager = getSystemService(WifiManager::class.java)
    val hasStaConcurrency = Build.VERSION.SDK_INT >= 31 &&
            wifiManager.isStaConcurrencyForLocalOnlyConnectionsSupported
    val hasStaApConcurrency = Build.VERSION.SDK_INT >= 30 &&
            wifiManager.isStaApConcurrencySupported
    val hasWifiAwareSupport = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)

    return buildString {
        append("Meshrabiya: Version :${MeshrabiyaConstants.VERSION}\n")
        append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        append("Device: ${Build.MANUFACTURER} - ${Build.MODEL}\n")
        append("5Ghz supported: ${wifiManager.is5GHzBandSupported}\n")
        append("Local-only station concurrency: $hasStaConcurrency\n")
        append("Station-AP concurrency: $hasStaApConcurrency\n")
        append("WifiAware support: $hasWifiAwareSupport\n")
    }
}