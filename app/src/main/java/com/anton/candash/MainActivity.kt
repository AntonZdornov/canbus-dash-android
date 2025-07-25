package com.anton.candash

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import com.anton.candash.services.BleService
import com.anton.candash.ui.components.PipScreen
import com.anton.candash.ui.screens.MainScreen
import com.anton.candash.ui.theme.ApptestTheme
import com.anton.candash.viewmodels.MainViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private lateinit var bleService: BleService
    private var serviceBound = false

    private val viewModel: MainViewModel by viewModels()
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Intent(this, BleService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            ApptestTheme {
                if (isInPipModeState.value) {
                    // UI для PIP
                    PipScreen(
                        bleValue = viewModel.bleValue,
                    )
                } else {
                    // Твой обычный экран
                    MainScreen(
                        bleValue = viewModel.bleValue,
                        onStartScan = { viewModel.startScan() }
                    )
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BleService.LocalBinder
            bleService = binder.getService()
            serviceBound = true
            Log.d("test", "in")
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)

        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun setPictureInPictureParams(params: PictureInPictureParams) {
        super.setPictureInPictureParams(params)
        val aspectRatio = Rational(1, 1)
        val pipBuilder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
        enterPictureInPictureMode(pipBuilder.build())
    }

}