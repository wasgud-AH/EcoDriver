package com.example.ecodriver.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.DB_NAME
import com.example.ecodriver.data.EcoDb
import com.example.ecodriver.data.csvRow
import com.example.ecodriver.ui.components.ConfirmDialog
import com.example.ecodriver.ui.components.EcoCard
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.GreenButton
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun DataManagementScreen() {
    val context = LocalContext.current
    val db = EcoDb.db
    val scope = rememberCoroutineScope()
    val vehicles by remember { db.vehicles().count() }.collectAsState(0)
    val trips by remember { db.trips().count() }.collectAsState(0)
    val samples by remember { db.samples().count() }.collectAsState(0)
    val events by remember { db.events().count() }.collectAsState(0)
    val sizeBytes = remember(trips, samples, events) { dbSizeBytes(context) }

    fun export(uri: Uri?, rows: suspend () -> List<String>) {
        uri ?: return
        scope.launch {
            val msg = try {
                val lines = rows()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { w -> lines.forEach { w.appendLine(it) } }
                }
                "Exported ${lines.size - 1} rows"
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val tripsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        export(uri) {
            val names = db.vehicles().all().first().associate { it.id to it.displayName }
            listOf(csvRow(TRIP_HEADER)) + db.trips().all().map { t ->
                csvRow(listOf(t.id, names[t.vehicleId], t.startMs, t.endMs, t.distanceMi, t.fuelGal, t.mpg, t.score,
                    t.consistency, t.idleSec, t.harshAccelCount, t.harshBrakeCount, t.dfcoSec))
            }
        }
    }
    val samplesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        export(uri) {
            // shortcut: loads every sample into memory; stream with a paged query if logs reach millions of rows
            listOf(csvRow(SAMPLE_HEADER)) + db.samples().all().map { s ->
                csvRow(listOf(s.tripId, s.timeMs, s.rpm, s.mph, s.throttlePct, s.loadPct, s.coolantF, s.mafGps, s.mapKpa))
            }
        }
    }

    DataManagementContent(
        vehicles, trips, samples, events, sizeBytes,
        onExportTrips = { tripsLauncher.launch("ecodriver-trips.csv") },
        onExportSamples = { samplesLauncher.launch("ecodriver-samples.csv") },
        onDeleteTrips = { scope.launch { db.trips().deleteAll() } }, // samples and events cascade
    )
}

private val TRIP_HEADER = listOf("trip_id", "vehicle", "start_ms", "end_ms", "distance_mi", "fuel_gal", "mpg", "score",
    "consistency", "idle_sec", "harsh_accel", "harsh_brake", "dfco_sec")
private val SAMPLE_HEADER = listOf("trip_id", "time_ms", "rpm", "mph", "throttle_pct", "load_pct", "coolant_f", "maf_gps", "map_kpa")

private fun dbSizeBytes(context: Context): Long {
    val db = context.getDatabasePath(DB_NAME)
    return listOf(db, File(db.path + "-wal"), File(db.path + "-shm")).sumOf { it.length() }
}

private fun sizeText(bytes: Long): String =
    if (bytes < 1024 * 1024) String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

@Composable
private fun DataManagementContent(
    vehicles: Int,
    trips: Int,
    samples: Int,
    events: Int,
    sizeBytes: Long,
    onExportTrips: () -> Unit,
    onExportSamples: () -> Unit,
    onDeleteTrips: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Background).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EcoTitle("Data")
        EcoCard(Modifier.fillMaxWidth()) {
            Text("Storage", color = TextSecondary, fontSize = 14.sp)
            listOf("Vehicles" to "$vehicles", "Trips" to "$trips", "Samples" to "$samples", "Events" to "$events",
                "Database size" to sizeText(sizeBytes)).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(value, color = TextPrimary, fontSize = 16.sp)
                }
            }
        }
        GreenButton("Export trips (CSV)", Modifier.fillMaxWidth(), enabled = trips > 0, onClick = onExportTrips)
        GreenButton("Export samples (CSV)", Modifier.fillMaxWidth(), enabled = samples > 0, onClick = onExportSamples)
        GreenButton("Delete all trips", Modifier.fillMaxWidth(), enabled = trips > 0) { confirmDelete = true }
        Text("Vehicles are kept.", color = TextSecondary, fontSize = 14.sp)
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete all trips?",
            body = "Deletes $trips trip${if (trips == 1) "" else "s"}. This can't be undone.",
            confirmText = "Delete",
            onConfirm = onDeleteTrips,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun DataManagementPreview() {
    DataManagementContent(2, 37, 41_210, 58, 3_400_000, {}, {}, {})
}
