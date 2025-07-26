package com.greybox.projectmesh.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ustadmobile.meshrabiya.MeshrabiyaConstants.LOG_TAG
import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState
import java.util.regex.Pattern


fun interface ConnectBluetoothLauncher {
    fun launch(bluetoothConfig: MeshrabiyaBluetoothState)
}

data class ConnectBluetoothLauncherResult(
    val device: BluetoothDevice?,
)

/**
*/
@SuppressLint("MissingPermission")
@Composable
    fun rememberBluetoothConnectLauncher(
onResult: (ConnectBluetoothLauncherResult) -> Unit,
) : ConnectBluetoothLauncher {

    val context = LocalContext.current

    return ConnectBluetoothLauncher { config ->
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter?.isEnabled == true) {
            val bondedDevices = bluetoothAdapter.bondedDevices

            // Show a simple selection dialog with bonded devices
            // Or automatically try the first bonded device for testing
            val testDevice = bondedDevices.firstOrNull()
            onResult(ConnectBluetoothLauncherResult(testDevice))
        } else {
            onResult(ConnectBluetoothLauncherResult(null))
        }
    }
}