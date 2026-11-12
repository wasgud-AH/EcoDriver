package com.example.ecodriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.ConnectionState
import com.example.ecodriver.data.DiagRequest
import com.example.ecodriver.data.Pid
import com.example.ecodriver.data.TripState
import com.example.ecodriver.data.diagRequest
import com.example.ecodriver.data.durationText
import com.example.ecodriver.data.ecuStatus
import com.example.ecodriver.data.kmhToMph
import com.example.ecodriver.data.obdValues
import com.example.ecodriver.data.parseDtcs
import com.example.ecodriver.ui.components.ConfirmDialog
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.GreenButton
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.components.WaitingBanner
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DiagnosticToolsScreen() {
    DiagnosticContent(
        connected = ecuStatus == ConnectionState.Connected,
        stopped = TripState.mph == 0.0,
        busy = diagRequest != null,
        values = obdValues,
        onRescan = { diagRequest = DiagRequest.Rescan },
        onClear = { diagRequest = DiagRequest.Clear },
    )
}

@Composable
private fun DiagnosticContent(
    connected: Boolean,
    stopped: Boolean,
    busy: Boolean,
    values: Map<String, String>,
    onRescan: () -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val mil = values[Pid.MIL]

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EcoTitle("Diagnostics")
        if (!connected) WaitingBanner()

        EcoCard(Modifier.fillMaxWidth()) {
            StatusRow("Check engine light", when (mil) { "true" -> "ON"; "false" -> "Off"; else -> "—" },
                if (mil == "true") Amber else TextPrimary)
            StatusRow("Trouble codes", values[Pid.DTC_COUNT] ?: "—")
            StatusRow("Since codes cleared",
                values[Pid.KM_SINCE_CLEAR]?.toDoubleOrNull()?.let { String.format(Locale.US, "%.0f mi", kmhToMph(it)) } ?: "—")
            StatusRow("Time since cleared",
                values[Pid.MIN_SINCE_CLEAR]?.toDoubleOrNull()?.let { durationText((it * 60_000).toLong()) } ?: "—")
            StatusRow("VIN", values[Pid.VIN]?.ifBlank { null } ?: "—")
            StatusRow("Module voltage", values[Pid.VOLTAGE]?.let { "$it V" } ?: "—")
        }

        CodeList("Stored", values[Pid.DTC_STORED])
        CodeList("Pending", values[Pid.DTC_PENDING])
        CodeList("Permanent", values[Pid.DTC_PERMANENT])

        GreenButton(if (busy) "Scanning…" else "Re-scan", Modifier.fillMaxWidth(), enabled = connected && !busy, onClick = onRescan)
        GreenButton("Clear codes", Modifier.fillMaxWidth(), enabled = connected && stopped && !busy) { confirmClear = true }
        if (connected && !stopped) Text("Stop the car to clear codes.", color = TextSecondary, fontSize = 14.sp)
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear trouble codes?",
            body = "This turns off the check engine light and resets the readiness monitors. " +
                "The car can fail an emissions test until it has been driven enough for the monitors to finish again.",
            confirmText = "Clear codes",
            onConfirm = onClear,
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** raw is null when the code list hasn't been read yet (or the car didn't answer). */
@Composable
private fun CodeList(title: String, raw: String?) {
    val codes = parseDtcs(raw)
    EcoCard(Modifier.fillMaxWidth()) {
        Text("$title (${codes.size})", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        when {
            raw == null -> Text("Not read", color = TextSecondary, fontSize = 15.sp)
            codes.isEmpty() -> Text("None", color = TextSecondary, fontSize = 15.sp)
            else -> codes.forEach { Text(it, color = Amber, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun DiagnosticPreview() {
    DiagnosticContent(
        connected = true, stopped = true, busy = false,
        values = mapOf(
            Pid.MIL to "true", Pid.DTC_COUNT to "2", Pid.KM_SINCE_CLEAR to "412", Pid.MIN_SINCE_CLEAR to "530",
            Pid.VIN to "1HGCR2F3XEA000000", Pid.VOLTAGE to "14.12",
            Pid.DTC_STORED to "P0300,P0171", Pid.DTC_PENDING to "", Pid.DTC_PERMANENT to "P0300",
        ),
        onRescan = {}, onClear = {},
    )
}
