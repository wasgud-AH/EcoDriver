package com.example.ecodriver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ecodriver.data.ConnectionState
import com.example.ecodriver.data.ecuStatus
import com.example.ecodriver.data.elmStatus
import com.example.ecodriver.data.needsDevicePick
import com.example.ecodriver.data.pairedDevices
import com.example.ecodriver.data.rpmText
import com.example.ecodriver.ui.ObdViewModel
import com.example.ecodriver.ui.theme.ButtonGreen


@Composable
fun ConnectionStatus(obdViewModel: ObdViewModel, modifier: Modifier = Modifier){

    if (needsDevicePick) {
        AlertDialog(

            onDismissRequest = { needsDevicePick = false },
            confirmButton = {},
            modifier,
            dismissButton = {
                TextButton(onClick = { needsDevicePick = false }) { Text("Cancel") }
            },
            title = { Text("Pick your OBD adapter") },
            text = {
                val devices = pairedDevices()
                val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
                Column(Modifier.heightIn(max = maxListHeight).verticalScroll(rememberScrollState())) {
                    if (devices.isEmpty()) {
                        Text("No paired devices. Pair the adapter in Android Bluetooth settings first.")
                    }
                    devices.forEach { device ->
                        TextButton(onClick = { obdViewModel.pickDevice(device.address) }) {
                            Text("${device.name}\n Mac Address: ${device.address}", color = Color.White)
                        }
                    }
                }
            }
        )
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ){
        // ELM Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ELM connection:", color = Color.White)
            StatusText(elmStatus)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ECU Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ECU connection:", color = Color.White)
            StatusText(ecuStatus)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // RPM Row, updates while polling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("RPM:", color = Color.White)
            Text(rpmText, color = Color.White)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    Row(
        modifier.fillMaxSize().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {

        val disconnected = elmStatus == ConnectionState.Disconnected
        Button(onClick = {
            if (disconnected) obdViewModel.connect() else obdViewModel.disconnect()
        },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),

            ) {
            Text(if (disconnected) "Connect" else "Disconnect", color = Color.White)
        }
    }
}

@Composable
private fun StatusText(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.Disconnected -> Color.Red
        ConnectionState.Connecting -> Color.Yellow
        ConnectionState.Connected -> ButtonGreen
    }
    Text(state.name, color = color)
}
