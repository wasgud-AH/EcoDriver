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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ecodriver.ui.theme.ButtonGreen

/*
Tuesday Demo has this stuck as Connect until Bluetooth is added
 */
@Composable
fun ConnectionStatus(modifier: Modifier = Modifier){
    val context = LocalContext.current.applicationContext
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
            Text("Disconnected", color = Color.Red)
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
            Toast.makeText(context, "Being implemented Soon!", Toast.LENGTH_SHORT).show()
        },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),

            ) {
            Text("Connect", color = Color.White)
        }
    }
}

