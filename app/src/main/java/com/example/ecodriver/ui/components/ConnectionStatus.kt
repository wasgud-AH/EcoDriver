package com.example.ecodriver.ui.components

import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ecodriver.ui.theme.ButtonGreen
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ecodriver.data.connectToElm
import com.example.ecodriver.data.readObd
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue

/*
Tuesday Demo has this stuck as Connect until Bluetooth is added
 */


@Composable
fun ConnectionStatus(modifier: Modifier = Modifier){
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var elmStatus by remember { mutableStateOf("Disconnected") }
    var elmError by remember { mutableStateOf<String?>(null) }

    fun connect() = scope.launch {
        elmStatus = "Connecting..."
        elmError = null
        elmStatus = try {
            val socket = connectToElm(context)
            readObd(socket.inputStream, socket.outputStream)
            "Connected"
        } catch (e: Exception) {
            elmError = e.message
            "Failed"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) connect() else elmStatus = "Permission denied" }



    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ){
        // ELM Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text("ELM connection:", color = Color.White)
            Text(elmStatus, color = if (elmStatus == "Connected") Color.Green else Color.Red)
        }

        elmError?.let {
            Text(it, color = Color(0xFFFF8A80), modifier = Modifier.fillMaxWidth())
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            else connect()
        },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),

            ) {
            Text("Connect", color = Color.White)
        }
    }
}

