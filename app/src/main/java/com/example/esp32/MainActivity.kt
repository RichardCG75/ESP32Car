package com.example.esp32

import android.app.Activity
import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.util.UUID
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.esp32.ui.theme.ESP32Theme
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val targetDeviceAddress = "D8:BC:38:FC:A8:B2"
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ESP32Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                connectToDevice()
            }
        } else {
            connectToDevice()
        }
    }

    private fun connectToDevice() {
        bluetoothAdapter?.let { adapter ->
            val device: BluetoothDevice? = adapter.getRemoteDevice(targetDeviceAddress)
            device?.let {
                // Connect in a separate thread to avoid blocking the UI thread
                try {
                    bluetoothSocket = it.createRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket?.connect()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun sendData(data: Byte) {
        bluetoothSocket?.let {
            try {
                val outputStream: OutputStream = it.outputStream
                outputStream.write(byteArrayOf(data))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!checkPermissions(context)) {
                    requestPermissions(context)
                } else {
                    connectToDevice()
                }
            } else {
                connectToDevice()
            }
        }

        CarControllerScreen()
    }

    private fun checkPermissions(context: Context): Boolean {
        val fineLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(context: Context) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToDevice()
        }
    }

    @Composable
    fun CarControllerScreen() {
        var forwardPressed by remember { mutableStateOf(false) }
        var backwardPressed by remember { mutableStateOf(false) }
        var leftPressed by remember { mutableStateOf(false) }
        var rightPressed by remember { mutableStateOf(false) }
        var forwardEnabled by remember { mutableStateOf(true) }
        var backwardEnabled by remember { mutableStateOf(true) }
        var leftEnabled by remember { mutableStateOf(true) }
        var rightEnabled by remember { mutableStateOf(true) }
        var stoppedEnabled by remember { mutableStateOf(true) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    content = {

                        Button(
                            modifier = Modifier.align(Alignment.TopStart),
                            onClick = {
                                connectToDevice()
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                            },
                            content = { Text("Connect") }
                        )

                        // Move Forward Button
                        Button(
                            enabled = forwardEnabled,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(250.dp)
                                .height(250.dp),
                            onClick = {
                                backwardPressed = false
                                backwardEnabled = true
                                forwardPressed = !forwardPressed
                                forwardEnabled = !forwardPressed
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                                sendData(
                                    encodeBooleansToByte(
                                        forwardPressed,
                                        false,
                                        leftPressed,
                                        rightPressed,
                                        false
                                    )
                                )
                            },
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = ""
                                )
                            }
                        )

                        // Move Backward Button
                        Button(
                            enabled = backwardEnabled,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(250.dp)
                                .height(250.dp),
                            onClick = {
                                forwardPressed = false
                                forwardEnabled = true
                                backwardPressed = !backwardPressed
                                backwardEnabled = !backwardPressed
                                sendData(
                                    encodeBooleansToByte(
                                        false,
                                        backwardPressed,
                                        leftPressed,
                                        rightPressed,
                                        false
                                    )
                                )
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                            },
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = ""
                                )
                            }
                        )

                        // Stop Button
                        Button(
                            enabled = stoppedEnabled,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(90.dp)
                                .height(150.dp),
                            onClick = {
                                forwardPressed = false
                                forwardEnabled = true
                                backwardPressed = false
                                backwardEnabled = true
                                leftPressed = false
                                leftEnabled = true
                                rightPressed = false
                                rightEnabled = true
                                sendData(
                                    encodeBooleansToByte(
                                        false,
                                        false,
                                        false,
                                        false,
                                        true
                                    )
                                )
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                            },
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = ""
                                )
                            }
                        )

                        // Turn Left Button
                        Button(
                            enabled = leftEnabled,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(130.dp)
                                .height(250.dp),
                            onClick = {
                                rightPressed = false
                                rightEnabled = true
                                leftPressed = !leftPressed
                                leftEnabled = !leftPressed
                                sendData(
                                    encodeBooleansToByte(
                                        forwardPressed,
                                        backwardPressed,
                                        leftPressed,
                                        false,
                                        false
                                    )
                                )
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                            },
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Filled.KeyboardArrowLeft,
                                    contentDescription = ""
                                )
                            }
                        )

                        // Turn Right Button
                        Button(
                            enabled = rightEnabled,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(130.dp)
                                .height(250.dp),
                            onClick = {
                                leftPressed = false
                                leftEnabled = true
                                rightPressed = !rightPressed
                                rightEnabled = !rightPressed
                                sendData(
                                    encodeBooleansToByte(
                                        forwardPressed,
                                        backwardPressed,
                                        false,
                                        rightPressed,
                                        false
                                    )
                                )
                                Log.d(
                                    "Bluetooth",
                                    "Socket connected: ${bluetoothSocket?.isConnected}"
                                )
                            },
                            content = {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = ""
                                )
                            }
                        )
                    }
                )
            }
        )
    }
}

private fun encodeBooleansToByte(
    carIsMovingForward: Boolean,
    carIsMovingBackward: Boolean,
    carIsTurningLeft: Boolean,
    carIsTurningRight: Boolean,
    carIsStopped: Boolean
): Byte {
    var result = 0
    if (carIsMovingForward) {
        result = result or 0b00001
    }
    if (carIsMovingBackward) {
        result = result or 0b00010
    }
    if (carIsTurningLeft) {
        result = result or 0b00100
    }
    if (carIsTurningRight) {
        result = result or 0b01000
    }
    if (carIsStopped) {
        result = result or 0b10000
    }
    return result.toByte()
}

