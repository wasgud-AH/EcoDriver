package com.example.ecodriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.obdUnits
import com.example.ecodriver.data.obdValues
import com.example.ecodriver.ui.components.CardShape
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.CardBg
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary

@Composable
fun AllSensorsScreen() {
    AllSensorsContent(obdValues.entries.map { (name, v) -> name to "$v ${obdUnits[name].orEmpty()}".trim() })
}

@Composable
private fun AllSensorsContent(sensors: List<Pair<String, String>>) {
    val sorted = sensors.sortedBy { it.first }
    LazyColumn(
        Modifier.fillMaxSize().background(Background),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { EcoTitle("All Sensors") }
        item {
            Text(
                if (sorted.isEmpty()) "Connect to the car to see sensors"
                else "${sorted.size} sensor${if (sorted.size == 1) "" else "s"} reporting",
                color = TextSecondary, fontSize = 16.sp,
            )
        }
        items(sorted, key = { it.first }) { (name, value) ->
            Row(Modifier.fillMaxWidth().background(CardBg, CardShape).padding(16.dp)) {
                Text(name, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(value.ifBlank { "—" }, color = TextPrimary, fontSize = 15.sp, textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun AllSensorsPreview() {
    AllSensorsContent(listOf("Engine RPM" to "812 RPM", "Vehicle Speed" to "0 Km/h", "Engine Coolant Temperature" to "88 °C"))
}
