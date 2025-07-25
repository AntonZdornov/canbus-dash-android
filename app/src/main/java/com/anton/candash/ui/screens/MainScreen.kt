package com.anton.candash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anton.candash.ui.components.BatteryGauge
import com.anton.candash.ui.components.Greeting

@Composable
fun MainScreen(
    bleValue: MutableState<String>,
    onStartScan: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(name = bleValue.value)

            Spacer(modifier = Modifier.height(16.dp))

            BatteryGauge(
                modifier = Modifier.size(200.dp),
                percentage = 30
            )

            Button(onClick = { onStartScan() }) {
                Text("Start BLE Scan")
            }
        }
    }
}