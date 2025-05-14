package com.example.esp32_bt_led_controller.view_models

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.*

class BluetoothViewModel : ViewModel() {

    var discoveredDevices = mutableStateListOf<BluetoothDevice>()
        private set

    var isConnected by mutableStateOf(false)
        private set

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!discoveredDevices.contains(it) && it.name != null) {
                            discoveredDevices.add(it)
                            Log.d("BluetoothViewModel", "Device found: ${it.name}")
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("BluetoothViewModel", "Discovery started")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothViewModel", "Discovery finished")
                }
            }
        }
    }

    private fun finishReceiver(onFinished: (() -> Unit)?): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                    onFinished?.invoke()
                    try {
                        context?.unregisterReceiver(this)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDiscovery(context: Context, onFinished: (() -> Unit)? = null) {
        discoveredDevices.clear()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val finishFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        context.registerReceiver(receiver, filter)
        context.registerReceiver(finishReceiver(onFinished), finishFilter)

        bluetoothAdapter?.startDiscovery()
        Log.i("BluetoothViewModel", "Discovery started")
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun connectToDevice(device: BluetoothDevice, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bluetoothAdapter?.cancelDiscovery()
                val socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
                )
                socket.connect()
                bluetoothSocket = socket
                outputStream = socket.outputStream
                isConnected = true
                Log.d("BluetoothViewModel", "Connected to ${device.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                isConnected = false
                Log.e("BluetoothViewModel", "Connection failed: ${e.message}")
            }
        }
    }

    fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            Log.d("BluetoothViewModel", "Sent data: $data")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("BluetoothViewModel", "Failed to send data: ${e.message}")
        }
    }

    fun cleanup(context: Context) {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            }
        } catch (_: Exception) {}

        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (_: Exception) {}
        isConnected = false
    }
}
