package com.example.ecodriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.ConnectionState
import com.example.ecodriver.data.Pid
import com.example.ecodriver.data.cToF
import com.example.ecodriver.data.ecuStatus
import com.example.ecodriver.data.kmhToMph
import com.example.ecodriver.data.kpaToPsi
import com.example.ecodriver.data.obdNumbers
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.LineChart
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.components.WaitingBanner
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

private const val HISTORY_S = 60

private data class LivePid(val label: String, val key: String, val decimals: Int, val convert: (Double) -> Double = { it })

private val livePids = listOf(
    LivePid("RPM", Pid.RPM, 0),
    LivePid("Speed (mph)", Pid.SPEED, 0, ::kmhToMph),
    LivePid("Throttle %", Pid.THROTTLE, 0),
    LivePid("Load %", Pid.LOAD, 0),
    LivePid("Coolant °F", Pid.COOLANT, 0, ::cToF),
    LivePid("MAF g/s", Pid.MAF, 1),
    LivePid("MAP psi", Pid.MAP, 1, ::kpaToPsi),
    LivePid("STFT B1 %", Pid.STFT_B1, 1),
    LivePid("LTFT B1 %", Pid.LTFT_B1, 1),
)

@Composable
fun LiveDataScreen() {
    val history = remember { mutableStateMapOf<String, List<Float>>() }
    LaunchedEffect(Unit) {
        while (true) {
            for (p in livePids) {
                val v = obdNumbers[p.key] ?: continue
                history[p.key] = (history[p.key].orEmpty() + p.convert(v).toFloat()).takeLast(HISTORY_S)
            }
            delay(1000)
        }
    }
    LiveDataContent(
        connected = ecuStatus == ConnectionState.Connected,
        current = livePids.associate { p -> p.key to obdNumbers[p.key]?.let(p.convert) },
        history = history,
    )
}

@Composable
private fun LiveDataContent(connected: Boolean, current: Map<String, Double?>, history: Map<String, List<Float>>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = ScreenPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { EcoTitle("Live Data") }
        if (!connected) item(span = { GridItemSpan(maxLineSpan) }) { WaitingBanner() }
        items(livePids, key = { it.key }) { p ->
            val value = current[p.key]
            EcoCard {
                Text(p.label, color = TextSecondary, fontSize = 14.sp)
                Text(
                    value?.let { String.format(Locale.US, "%.${p.decimals}f", it) } ?: "—",
                    color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.SemiBold,
                )
                val h = history[p.key].orEmpty()
                if (value != null && h.size >= 2) {
                    LineChart(
                        xs = h.indices.map { it.toFloat() }, ys = h,
                        modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 4.dp),
                        xRange = 0f..(HISTORY_S - 1).toFloat(),
                        yMin = h.min(), yMax = h.max(),
                    )
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun LiveDataPreview() {
    val current = livePids.associate { it.key to 42.0 } + (Pid.LTFT_B1 to null)
    val history = livePids.associate { p -> p.key to List(HISTORY_S) { i -> (40 + 10 * kotlin.math.sin(i / 6.0)).toFloat() } }
    LiveDataContent(connected = false, current = current, history = history)
}
