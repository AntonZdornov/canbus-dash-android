package com.anton.candash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.anton.candash.services.BleService
import com.anton.candash.ui.screens.MainScreen
import com.anton.candash.ui.theme.ApptestTheme
import com.anton.candash.viewmodels.MainViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private lateinit var bleService: BleService
    private  var serviceBound = false

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Intent(this, BleService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            ApptestTheme {
                MainScreen(
                    bleValue = viewModel.bleValue,
                    onStartScan = { viewModel.startScan() }
                )
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BleService.LocalBinder
            bleService = binder.getService()
            serviceBound = true
            viewModel.setBleService(bleService)
            viewModel.startScan()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}