package com.example.ecodriver.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.EcoDb
import com.example.ecodriver.data.Trip
import com.example.ecodriver.data.Vehicle
import com.example.ecodriver.data.activeVehicleId
import com.example.ecodriver.data.durationText
import com.example.ecodriver.data.savingsUsd
import com.example.ecodriver.data.tripMpgText
import com.example.ecodriver.data.usdText
import com.example.ecodriver.ui.components.CardShape
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.components.StatTile
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.CardBg
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TREND_TRIPS = 20

@Composable
fun StatisticsScreen(onOpenTrip: (Long) -> Unit) {
    val vid = activeVehicleId
    val db = EcoDb.db
    val vehicle by remember(vid) { db.vehicles().watch(vid) }.collectAsState(null)
    val trips by remember(vid) { db.trips().forVehicle(vid) }.collectAsState(emptyList())
    StatisticsContent(vehicle, trips, monthStartMs(), onOpenTrip)
}

private fun monthStartMs(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Savings for one trip. Null when the trip has no fuel data or the vehicle is unknown. */
fun tripSavings(t: Trip, v: Vehicle?): Double? =
    if (v == null || t.fuelGal <= 0) null else savingsUsd(t.distanceMi, t.fuelGal, v.baselineMpg, v.fuelPricePerGal)

@Composable
private fun StatisticsContent(vehicle: Vehicle?, trips: List<Trip>, monthStartMs: Long, onOpenTrip: (Long) -> Unit) {
    val fueled = trips.filter { it.fuelGal > 0 }
    val lifetimeMpg = if (fueled.isEmpty()) null else fueled.sumOf { it.distanceMi } / fueled.sumOf { it.fuelGal }
    val monthSaved = trips.filter { it.startMs >= monthStartMs }.mapNotNull { tripSavings(it, vehicle) }.sum()

    LazyColumn(
        Modifier.fillMaxSize().background(Background),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EcoTitle("Statistics")
            Text(vehicle?.displayName ?: "", color = TextSecondary, fontSize = 14.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Lifetime MPG", tripMpgText(lifetimeMpg), Modifier.weight(1f), ButtonGreen)
                StatTile("Total miles", String.format(Locale.US, "%.1f", trips.sumOf { it.distanceMi }), Modifier.weight(1f))
                StatTile("Trips", "${trips.size}", Modifier.weight(1f))
            }
        }
        item {
            EcoCard(Modifier.fillMaxWidth()) {
                Text(
                    if (monthSaved >= 0) "This month you saved ${usdText(monthSaved)}"
                    else "This month cost ${usdText(monthSaved)} extra",
                    color = if (monthSaved >= 0) ButtonGreen else Amber,
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                )
                Text("Compared with the baseline MPG", color = TextSecondary, fontSize = 14.sp)
            }
        }
        item {
            EcoCard(Modifier.fillMaxWidth()) {
                Text("MPG, last $TREND_TRIPS trips", color = TextSecondary, fontSize = 14.sp)
                MpgBars(
                    trips.take(TREND_TRIPS).reversed().map { it.mpg ?: 0.0 },
                    vehicle?.baselineMpg,
                    Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp),
                )
                Text("- - - baseline", color = TextSecondary, fontSize = 12.sp)
            }
        }
        item { Text("Trips", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
        if (trips.isEmpty()) item { Text("No trips yet. Drive with the adapter connected.", color = TextSecondary) }
        items(trips, key = { it.id }) { t -> TripRow(t) { onOpenTrip(t.id) } }
    }
}

@Composable
private fun MpgBars(mpgs: List<Double>, baseline: Double?, modifier: Modifier) {
    Canvas(modifier) {
        if (mpgs.isEmpty()) return@Canvas
        val top = (listOfNotNull(mpgs.max(), baseline).max() * 1.15).toFloat()
        if (top <= 0f) return@Canvas
        val slot = size.width / TREND_TRIPS
        mpgs.forEachIndexed { i, mpg ->
            val h = (mpg.toFloat() / top) * size.height
            drawRoundRect(
                ButtonGreen,
                topLeft = Offset(i * slot + slot * 0.15f, size.height - h),
                size = Size(slot * 0.7f, h),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
        baseline?.let {
            val y = size.height - (it.toFloat() / top) * size.height
            drawLine(
                TextSecondary, Offset(0f, y), Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            )
        }
    }
}

private val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)

@Composable
private fun TripRow(t: Trip, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardBg, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(dateFmt.format(Date(t.startMs)), color = TextPrimary, fontSize = 16.sp)
            Text(
                String.format(Locale.US, "%.1f mi · %s · score %d", t.distanceMi, durationText(t.endMs - t.startMs), t.score.toInt()),
                color = TextSecondary, fontSize = 14.sp,
            )
        }
        Text("${tripMpgText(t.mpg)} MPG", color = ButtonGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun sampleTrips(): List<Trip> = List(8) { i ->
    val start = 1_762_000_000_000L - i * 86_400_000L
    val mpg = 24.0 + (i * 7 % 11)
    val dist = 5.0 + i * 1.7
    Trip(
        id = i + 1L, vehicleId = 1, startMs = start, endMs = start + (12 + i * 3) * 60_000L,
        distanceMi = dist, fuelGal = dist / mpg, mpg = mpg, score = 70.0 + i * 3, consistency = 80.0,
        idleSec = 45.0, harshAccelCount = i % 3, harshBrakeCount = i % 2, dfcoSec = 60.0,
    )
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun StatisticsPreview() {
    StatisticsContent(Vehicle(1, "Honda", "Civic", 2014, 13.2, 30.0, 3.49), sampleTrips(), 0L) {}
}
