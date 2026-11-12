package com.example.ecodriver.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.ecodriver.data.KEY_KEEP_SCREEN_ON
import com.example.ecodriver.data.ecoPrefs
import com.example.ecodriver.data.saveObdAddress
import com.example.ecodriver.data.savedObdAddress
import com.example.ecodriver.ui.ObdViewModel
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.GreenButton
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary

@Composable
fun SettingsScreen(obdViewModel: ObdViewModel) {
    val context = LocalContext.current.applicationContext
    var address by remember { mutableStateOf(savedObdAddress(context)) }
    var keepOn by remember { mutableStateOf(ecoPrefs(context).getBoolean(KEY_KEEP_SCREEN_ON, false)) }
    SettingsContent(
        address = address,
        keepScreenOn = keepOn,
        onForget = {
            saveObdAddress(context, null)
            address = null
            Toast.makeText(context, "OBD device forgotten. Tap Connect to pick another.", Toast.LENGTH_SHORT).show()
        },
        onDisconnect = { obdViewModel.disconnect() },
        onKeepScreenOn = {
            keepOn = it
            ecoPrefs(context).edit { putBoolean(KEY_KEEP_SCREEN_ON, it) }
        },
    )
}

@Composable
private fun SettingsContent(
    address: String?,
    keepScreenOn: Boolean,
    onForget: () -> Unit,
    onDisconnect: () -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Background).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EcoTitle("Settings")
        EcoCard(Modifier.fillMaxWidth()) {
            Text("OBD adapter", color = TextSecondary, fontSize = 14.sp)
            Text(address ?: "No adapter saved", color = TextPrimary, fontSize = 18.sp)
        }
        GreenButton("Disconnect", Modifier.fillMaxWidth(), onClick = onDisconnect)
        GreenButton("Forget OBD device", Modifier.fillMaxWidth(), onClick = onForget)
        EcoCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Keep screen on while on Dashboard", color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOn,
                    colors = SwitchDefaults.colors(checkedTrackColor = ButtonGreen),
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun SettingsPreview() {
    SettingsContent("00:1D:A5:68:98:8B", keepScreenOn = true, onForget = {}, onDisconnect = {}, onKeepScreenOn = {})
}
