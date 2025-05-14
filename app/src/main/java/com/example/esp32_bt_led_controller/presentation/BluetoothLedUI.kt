package com.example.esp32_bt_led_controller.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.esp32_bt_led_controller.view_models.BluetoothViewModel

@Composable
@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
fun BluetoothLedUI(viewModel: BluetoothViewModel) {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    var permissionsGranted by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted && bluetoothEnabled) {
            isScanning = true
            viewModel.startDiscovery(context) {
                isScanning = false
            }
        }
    }

    // Check Bluetooth state when UI starts
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            permissionsGranted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!bluetoothEnabled) {
            Text(
                text = "Bluetooth is disabled. Please enable it.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                context.startActivity(intent)
            }) {
                Text("Enable Bluetooth")
            }
        } else {
            Button(
                onClick = {
                    if (permissionsGranted) {
                        isScanning = true
                        viewModel.startDiscovery(context) {
                            isScanning = false
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    }
                }
            ) {
                Text("Scan for Devices")
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Available Devices:", style = MaterialTheme.typography.headlineSmall)

            LazyColumn {
                items(viewModel.discoveredDevices) { device ->
                    DeviceItem(device = device, onClick = {
                        viewModel.connectToDevice(device, context)
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    viewModel.isConnected -> "Status: Connected to device"
                    else -> "Status: Not Connected"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.sendData("1") },
                enabled = viewModel.isConnected
            ) {
                Text("Turn ON LED")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.sendData("0") },
                enabled = viewModel.isConnected
            ) {
                Text("Turn OFF LED")
            }
        }
    }
}

@Composable
@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(text = device.name ?: "Unknown Device")
            Spacer(Modifier.weight(1f))
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}
