package com.example.ecodriver.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ecodriver.data.saveObdAddress
import com.example.ecodriver.ui.theme.ButtonGreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = {
                saveObdAddress(context, null)
                Toast.makeText(context, "OBD device forgotten. Tap Connect to pick another.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
        ) {
            Text("Forget OBD device", color = Color.White)
        }
    }
}
