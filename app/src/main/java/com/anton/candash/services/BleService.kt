package com.anton.candash.services

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.UUID

class BleService : Service() {
    private val binder = LocalBinder();

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var onResultCallback: ((String) -> Unit)? = null

    private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val CHARACTERISTIC_UUID_SOC = UUID.fromString("abcd1234-abcd-1234-abcd-1234567890ab")
    private val CHARACTERISTIC_UUID_RMP = UUID.fromString("abcd1234-abcd-1234-abcd-1234567890ac")

    private val targetDeviceAddress = "70:04:1D:38:75:76"

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "Found device: ${result.device.address}")

            if (result.device.address.equals(targetDeviceAddress, ignoreCase = true)) {
                Log.d(TAG, "Target device found")
                stopScan()
                connectToDevice(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "Scan failed: $errorCode")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
    }

    fun startScan(callback: (String) -> Unit) {
        onResultCallback = callback

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            bleScanner.startScan(scanCallback)
            Log.d(TAG, "Scanning for device $targetDeviceAddress...")
        } else {
            Log.d(TAG, "Scan stopped")
        }
    }

    fun stopScan() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            bleScanner.stopScan(scanCallback)
            Log.d(TAG, "Scan stopped")
        }
    }


    private fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
        } else {
            Log.d(TAG, "No BLE connect permission")
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID_SOC)

            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, true)
                val cccDescriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccDescriptor)

                Log.d(TAG, "Subscribed to notifications")
            } else {
                Log.d(TAG, "Characteristic not found")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            val decValue = data.firstOrNull()?.toUByte()?.toInt() ?: return
            Log.d(TAG, "Received value: $decValue")
            onResultCallback?.invoke("Received: $decValue")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    companion object {
        const val TAG = "BleService"
    }
}