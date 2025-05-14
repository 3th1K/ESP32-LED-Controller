package com.example.esp32_bt_led_controller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.esp32_bt_led_controller.presentation.BluetoothLedUI
import com.example.esp32_bt_led_controller.ui.theme.ESP32BTLEDControllerTheme
import com.example.esp32_bt_led_controller.view_models.BluetoothViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { BluetoothViewModel() }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()
        setContent {
            MaterialTheme {
                BluetoothLedUI(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup(this)
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        }
    }
}