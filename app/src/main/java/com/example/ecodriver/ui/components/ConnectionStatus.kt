package com.example.ecodriver.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ecodriver.data.runBluetoothObdSession
import com.example.ecodriver.ui.theme.ButtonGreen
import kotlinx.coroutines.*
import androidx.compose.runtime.rememberCoroutineScope

/*
Tuesday Demo has this stuck as Connect until Bluetooth is added
 */

@Composable
fun ConnectionStatus(modifier: Modifier = Modifier){
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var elmStatus by remember { mutableStateOf("Disconnected") }
    val elmStatusColor = when (elmStatus) {
        "Connected" -> Color.Green
        "Connecting" -> Color.Yellow
        else -> Color.Red
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
            Text(elmStatus, color = elmStatusColor)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ECU Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ECU connection:", color = Color.White)
            Text("Disconnected", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    Row(
        modifier.fillMaxSize().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {

        Button(onClick = {
            scope.launch {
                elmStatus = "Connecting"
                val ok = runBluetoothObdSession(onConnected = { elmStatus = "Connected" })
                elmStatus = "Disconnected"
                Toast.makeText(
                    context,
                    if (ok) "OBD session OK" else "OBD session failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
            enabled = elmStatus == "Disconnected",
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),

            ) {
            Text("Connect", color = Color.White)
        }
    }
}

