package com.example.ecodriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import com.example.ecodriver.data.EcoDb
import com.example.ecodriver.data.Event
import com.example.ecodriver.data.EventType
import com.example.ecodriver.data.Sample
import com.example.ecodriver.data.Trip
import com.example.ecodriver.data.Vehicle
import com.example.ecodriver.data.durationText
import com.example.ecodriver.data.tripMpgText
import com.example.ecodriver.data.usdText
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.LineChart
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.components.StatTile
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.Blue
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TripReportScreen(tripId: Long) {
    val db = EcoDb.db
    val trip by remember(tripId) { db.trips().watch(tripId) }.collectAsState(null)
    val samples by remember(tripId) { db.samples().forTrip(tripId) }.collectAsState(emptyList())
    val events by remember(tripId) { db.events().forTrip(tripId) }.collectAsState(emptyList())
    val vid = trip?.vehicleId ?: 0L
    val vehicle by remember(vid) { db.vehicles().watch(vid) }.collectAsState(null)
    trip?.let { TripReportContent(it, vehicle, samples, events) }
}

@Composable
private fun TripReportContent(trip: Trip, vehicle: Vehicle?, samples: List<Sample>, events: List<Event>) {
    val durationMs = trip.endMs - trip.startMs
    val idlePct = if (durationMs > 0) trip.idleSec / (durationMs / 1000.0) * 100 else 0.0
    val savings = tripSavings(trip, vehicle)

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EcoTitle("Trip report")
        Row {
            Text(tripMpgText(trip.mpg), color = ButtonGreen, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(" MPG", color = TextPrimary, fontSize = 22.sp, modifier = Modifier.padding(top = 36.dp))
        }
        Text(
            String.format(Locale.US, "%.1f mi · %s", trip.distanceMi, durationText(durationMs)) +
                (savings?.let { if (it >= 0) " · saved ${usdText(it)}" else " · cost ${usdText(it)} extra" } ?: ""),
            color = TextSecondary, fontSize = 16.sp,
        )

        val tiles = listOf(
            "Smoothness" to "${trip.score.toInt()}",
            "Consistency" to (trip.consistency?.let { "${it.toInt()}" } ?: "—"),
            "Idle" to String.format(Locale.US, "%.0f%%", idlePct),
            "Fuel-cut coasting" to durationText((trip.dfcoSec * 1000).toLong()),
            "Harsh accel" to "${trip.harshAccelCount}",
            "Harsh brake" to "${trip.harshBrakeCount}",
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
            }
        }

        // Both traces share one time axis: seconds since the trip started
        val xs = samples.map { (it.timeMs - trip.startMs) / 1000f }
        val xRange = 0f..(xs.lastOrNull() ?: 0f).coerceAtLeast(1f)
        val harshXs = events.filter { it.type != EventType.IDLE_LONG }.map { (it.timeMs - trip.startMs) / 1000f }

        EcoCard(Modifier.fillMaxWidth()) {
            Text(
                String.format(Locale.US, "Speed (mph) · max %.0f", samples.maxOfOrNull { it.mph } ?: 0.0),
                color = TextSecondary, fontSize = 14.sp,
            )
            LineChart(xs, samples.map { it.mph.toFloat() }, Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp),
                xRange = xRange, markers = harshXs, markerColor = Amber)
            Text("● harsh event", color = Amber, fontSize = 12.sp)
        }
        EcoCard(Modifier.fillMaxWidth()) {
            Text("Throttle (%)", color = TextSecondary, fontSize = 14.sp)
            LineChart(xs, samples.map { it.throttlePct?.toFloat() }, Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp),
                color = Blue, xRange = xRange, yMax = 100f)
        }
        if (samples.isEmpty()) Text("No samples were logged for this trip.", color = TextSecondary)
    }
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun TripReportPreview() {
    val trip = sampleTrips().first()
    val samples = List(600) { i ->
        val mph = (35 + 25 * kotlin.math.sin(i / 40.0)).coerceAtLeast(0.0)
        Sample(i.toLong(), trip.id, trip.startMs + i * 1000L, 2000.0, mph, 10 + 15 * kotlin.math.cos(i / 25.0), 40.0, 195.0, 8.0, 50.0)
    }
    val events = listOf(
        Event(1, trip.id, EventType.HARSH_BRAKE, trip.startMs + 200_000, -9.0),
        Event(2, trip.id, EventType.HARSH_ACCEL, trip.startMs + 420_000, 8.0),
    )
    TripReportContent(trip, Vehicle(1, "Honda", "Civic", 2014, 13.2, 30.0, 3.49), samples, events)
}
