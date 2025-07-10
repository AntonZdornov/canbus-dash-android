package com.anton.candash

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.anton.candash.LOG
import com.anton.candash.ui.theme.ApptestTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    // UUID сервиса и характеристики (замени на свои, если другие)
    private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val CHARACTERISTIC_UUID = UUID.fromString("abcd1234-abcd-1234-abcd-1234567890ab")

    // MAC-адрес целевого BLE-устройства
    private val targetDeviceAddress = "70:04:1D:38:75:76"

    private val bleValue = mutableStateOf("Checking permissions...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ApptestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = bleValue.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        val permissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d(LOG, "All permissions granted")
                setupBluetooth(bleValue)
            } else {
                Log.e(LOG, "Permissions denied")
                bleValue.value = "Permissions denied"
            }
        }

        if (!hasBlePermissions()) {
            permissionsLauncher.launch(getRequiredPermissions())
        } else {
            setupBluetooth(bleValue)
        }
    }

    private fun hasBlePermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions.toTypedArray()
    }

    private fun setupBluetooth(bleValue: MutableState<String>) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        startScan(bleValue)
    }

    private fun startScan(bleValue: MutableState<String>) {
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                Log.d(LOG, "Found device: ${result.device.address}")

                if (result.device.address.equals(targetDeviceAddress, ignoreCase = true)) {
                    Log.d(LOG, "Target device found: ${result.device.address}")
                    stopSafeScan(this)
                    connectToDevice(result.device, bleValue)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(LOG, "Scan failed with error: $errorCode")
                bleValue.value = "Scan failed: $errorCode"
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        ) {
            bleScanner.startScan(scanCallback)
            Log.d(LOG, "Scanning started")
            bleValue.value = "Scanning6 for device $targetDeviceAddress..."
        } else {
            Log.e(LOG, "BLUETOOTH_SCAN permission not granted")
            bleValue.value = "No BLE scan permission"
        }
    }

    private fun stopSafeScan(scanCallback: ScanCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        ) {
            bleScanner.stopScan(scanCallback)
            Log.d(LOG, "Scanning stopped")
        }
    }

    private fun connectToDevice(device: BluetoothDevice, bleValue: MutableState<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(LOG, "Connected to GATT server")
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(LOG, "Disconnected from GATT server")
                        bleValue.value = "Disconnected"
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    val service = gatt.getService(SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        // Подписка на уведомления — запись дескриптора 0x2902
                        val cccDescriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(cccDescriptor)

                        bleValue.value = "Subscribed to notifications"
                        Log.d(LOG, "Notification subscription initiated")
                    } else {
                        bleValue.value = "Characteristic not found"
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    val data = characteristic.value
                    val decValue = data.firstOrNull()?.toUByte()?.toInt() ?: return
                    Log.d(LOG, "Received value: $decValue")
                    bleValue.value = "Received: $decValue"
                }
            })
        } else {
            Log.e(LOG, "BLUETOOTH_CONNECT permission not granted")
            bleValue.value = "No BLE connect permission"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                bluetoothGatt?.close()
                Log.d(LOG, "GATT connection closed")
            } catch (e: SecurityException) {
                Log.e(LOG, "Permission denied to close GATT", e)
            }
        } else {
            Log.e(LOG, "No permission to close GATT")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ApptestTheme {
        Greeting("Hello Android!")
    }
}
