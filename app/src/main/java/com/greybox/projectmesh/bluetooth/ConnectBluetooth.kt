package com.greybox.projectmesh.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ustadmobile.meshrabiya.vnet.bluetooth.MeshrabiyaBluetoothState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import com.greybox.projectmesh.extension.hasBluetoothConnectPermission

fun interface ConnectBluetoothLauncher {
    fun launch(bluetoothConfig: MeshrabiyaBluetoothState)
}

data class ConnectBluetoothLauncherResult(
    val macAddress: String?,
    val deviceName: String?,
)

@SuppressLint("MissingPermission")
@Composable
fun rememberBluetoothConnectLauncher(
    onResult: (ConnectBluetoothLauncherResult) -> Unit,
) : ConnectBluetoothLauncher {

    val context = LocalContext.current

    // ★ Keep latest result across recompositions
    val latestOnResult by rememberUpdatedState(onResult)


    var showDialog by remember { mutableStateOf(false) }
    val bondedDevicesState = remember { mutableStateListOf<BluetoothDevice>() }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                latestOnResult(ConnectBluetoothLauncherResult(null, null)) // ★ use latest
            }
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 420.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select a Bluetooth device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    if (bondedDevicesState.isEmpty()) {
                        Text(
                            text = "No paired devices found.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Pair a device in system settings, then try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    showDialog = false
                                    latestOnResult(ConnectBluetoothLauncherResult(null, null))
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .fillMaxWidth()
                        ) {
                            items(
                                items = bondedDevicesState,
                                key = { it.address } // stable key
                            ) { device ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // make sure there is only a single result
                                            if (showDialog) {
                                                showDialog = false
                                                latestOnResult(
                                                    ConnectBluetoothLauncherResult(
                                                        macAddress = device.address,
                                                        deviceName = device.name
                                                    )
                                                )
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        val displayName = device.name ?: "Unknown device"
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    showDialog = false
                                    latestOnResult(ConnectBluetoothLauncherResult(null, null))
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }

    return ConnectBluetoothLauncher { config ->
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter?.isEnabled == true) {

            // make sure we have permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !context.hasBluetoothConnectPermission()) {
                // if not just return null
                showDialog = false
                latestOnResult(ConnectBluetoothLauncherResult(null, null))
                return@ConnectBluetoothLauncher
            }

            val bondedDevices = bluetoothAdapter.bondedDevices
                .orEmpty()
                .toList()
                .sortedWith(compareBy<BluetoothDevice> { it.name ?: "" }.thenBy { it.address })

            bondedDevicesState.clear()
            bondedDevicesState.addAll(bondedDevices)

            showDialog = true
        } else {
            latestOnResult(ConnectBluetoothLauncherResult(null, null))
        }
    }
}