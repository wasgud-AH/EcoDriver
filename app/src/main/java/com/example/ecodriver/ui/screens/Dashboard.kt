package com.example.ecodriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.ConnectionState
import com.example.ecodriver.data.Cue
import com.example.ecodriver.data.HarshEvent
import com.example.ecodriver.data.KEY_KEEP_SCREEN_ON
import com.example.ecodriver.data.TripState
import com.example.ecodriver.data.ecoPrefs
import com.example.ecodriver.data.ecuStatus
import com.example.ecodriver.data.elapsedText
import com.example.ecodriver.data.instantMpg
import com.example.ecodriver.data.instantMpgText
import com.example.ecodriver.data.tripMpg
import com.example.ecodriver.data.tripMpgText
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.Blue
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.CardBg
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import java.util.Locale

private val TripFill = Color(0xFF5B5D61)
private val DarkOnColor = Color(0xFF1A1200)
private const val SCALE_MAX_MPG = 50.0

@Composable
fun DashboardScreen() {
    val view = LocalView.current
    val keepOn = remember { ecoPrefs(view.context).getBoolean(KEY_KEEP_SCREEN_ON, false) }
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    GlanceDashboard(
        connected = ecuStatus == ConnectionState.Connected,
        elapsedMs = TripState.nowMs - TripState.startMs,
        distanceMi = TripState.distanceMi,
        instantMpg = instantMpg(TripState.mph, TripState.galPerHour),
        tripMpg = tripMpg(TripState.distanceMi, TripState.fuelGal),
        cue = TripState.cue,
    )
}

@Composable
private fun GlanceDashboard(
    connected: Boolean,
    elapsedMs: Long,
    distanceMi: Double,
    instantMpg: Double?,
    tripMpg: Double?,
    cue: Cue,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp)
    ) {
        TopRow(connected, elapsedMs, distanceMi)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("NOW", color = TextSecondary, fontSize = 16.sp, letterSpacing = 3.sp)
            Text(
                instantMpgText(instantMpg),
                color = ButtonGreen,
                fontSize = 168.sp,
                lineHeight = 168.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text("MPG", color = TextPrimary, fontSize = 22.sp)
        }

        TripBar(instantMpg, tripMpg)
        Spacer(Modifier.height(32.dp))
        CueCard(cue)
    }
}

@Composable
private fun TopRow(connected: Boolean, elapsedMs: Long, distanceMi: Double) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (connected) ButtonGreen else TextSecondary, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (connected) "Logging · ${elapsedText(elapsedMs)}" else "Waiting for car",
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(String.format(Locale.US, "%.1f mi", distanceMi), color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun TripBar(instantMpg: Double?, tripMpg: Double?) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text("Trip average", color = TextSecondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${tripMpgText(tripMpg)} MPG", color = TextPrimary, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))

        // 20 dp tall so the marker can stand above and below the 10 dp track
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(CardBg, RoundedCornerShape(5.dp))
            )
            if (tripMpg != null) {
                Box(
                    Modifier
                        .fillMaxWidth(scaleFraction(tripMpg))
                        .height(10.dp)
                        .background(TripFill, RoundedCornerShape(5.dp))
                )
            }
            if (instantMpg != null) {
                Box(
                    Modifier
                        .offset(x = (maxWidth - 4.dp) * scaleFraction(instantMpg))
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(ButtonGreen, RoundedCornerShape(2.dp))
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("0", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${SCALE_MAX_MPG.toInt()} MPG", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

private fun scaleFraction(mpg: Double): Float = (mpg / SCALE_MAX_MPG).coerceIn(0.0, 1.0).toFloat()

@Composable
private fun CueCard(cue: Cue) {
    val (bg, icon, title, body) = when (cue) {
        is Cue.Harsh -> if (cue.event == HarshEvent.Brake) {
            CueStyle(Amber, "!", "Harsh braking", "Ease off sooner before stops")
        } else {
            CueStyle(Amber, "!", "Hard acceleration", "Press the gas more gently")
        }
        is Cue.Idling -> CueStyle(Amber, "!", "Idling ${cue.seconds} s", "Turn the engine off if you'll wait longer.")
        Cue.Coasting -> CueStyle(Blue, "↓", "Coasting · fuel cut", "Nice, keep lifting early.")
        Cue.Steady -> CueStyle(CardBg, "✓", "Steady", "Smooth driving saves fuel.")
    }
    val fg = if (bg == CardBg) TextPrimary else DarkOnColor

    Row(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(20.dp))
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(fg.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = fg, fontSize = 16.sp)
        }
    }
}

private data class CueStyle(val bg: Color, val icon: String, val title: String, val body: String)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GlanceDashboardPreview() {
    GlanceDashboard(
        connected = true,
        elapsedMs = 12 * 60_000L + 48_000L,
        distanceMi = 6.3,
        instantMpg = 42.0,
        tripMpg = 29.6,
        cue = Cue.Harsh(HarshEvent.Brake),
    )
}
