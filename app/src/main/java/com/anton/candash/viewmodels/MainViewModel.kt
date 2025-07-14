package com.anton.candash.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.anton.candash.services.BleService

class MainViewModel : ViewModel() {

    private var bleService: BleService? = null

    var bleValue = mutableStateOf("Waiting for BLE...")
        private set

    fun setBleService(service: BleService) {
        bleService = service
    }

    fun startScan() {
        bleValue.value = "Starting scan..."
        bleService?.startScan() { result ->
            bleValue.value = result
        }
    }
}