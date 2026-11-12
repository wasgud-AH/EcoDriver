package com.example.ecodriver.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.data.EcoDb
import com.example.ecodriver.data.Vehicle
import com.example.ecodriver.data.activeVehicleId
import com.example.ecodriver.data.ensureActiveVehicle
import com.example.ecodriver.data.setActiveVehicle
import com.example.ecodriver.data.usdText
import com.example.ecodriver.ui.components.CardShape
import com.example.ecodriver.ui.components.ConfirmDialog
import com.example.ecodriver.ui.components.EcoTitle
import com.example.ecodriver.ui.components.GreenButton
import com.example.ecodriver.ui.components.ScreenPadding
import com.example.ecodriver.ui.components.ecoFieldColors
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.Background
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.CardBg
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun VehicleSelectorScreen() {
    val dao = EcoDb.db.vehicles()
    val vehicles by remember { dao.all() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    VehicleSelectorContent(
        vehicles = vehicles,
        activeId = activeVehicleId,
        onSelect = ::setActiveVehicle,
        onSave = { v -> scope.launch { if (v.id == 0L) dao.insert(v) else dao.update(v) } },
        onDelete = { v -> scope.launch { dao.delete(v); ensureActiveVehicle() } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VehicleSelectorContent(
    vehicles: List<Vehicle>,
    activeId: Long,
    onSelect: (Long) -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
) {
    var editing by remember { mutableStateOf<Vehicle?>(null) }
    var adding by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(Background),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { EcoTitle("Vehicles") }
        item { Text("Tap to select. Long-press to edit.", color = TextSecondary, fontSize = 14.sp) }
        items(vehicles, key = { it.id }) { v ->
            val active = v.id == activeId
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg, CardShape)
                    .border(2.dp, if (active) ButtonGreen else Color.Transparent, CardShape)
                    .combinedClickable(onClick = { onSelect(v.id) }, onLongClick = { editing = v })
                    .padding(16.dp)
            ) {
                Text(v.displayName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    String.format(Locale.US, "Baseline %.1f MPG · %s/gal", v.baselineMpg, usdText(v.fuelPricePerGal)),
                    color = TextSecondary, fontSize = 14.sp,
                )
                if (active) Text("Active", color = ButtonGreen, fontSize = 14.sp)
            }
        }
        item { GreenButton("Add vehicle", Modifier.fillMaxWidth()) { adding = true } }
    }

    if (adding || editing != null) {
        VehicleDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun VehicleDialog(
    initial: Vehicle?,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
) {
    fun Double.str() = if (this == 0.0) "" else toString()
    var make by remember { mutableStateOf(initial?.make ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var year by remember { mutableStateOf(initial?.year?.takeIf { it > 0 }?.toString() ?: "") }
    var tank by remember { mutableStateOf(initial?.tankGal?.str() ?: "") }
    var mpg by remember { mutableStateOf(initial?.baselineMpg?.str() ?: "") }
    var price by remember { mutableStateOf(initial?.fuelPricePerGal?.str() ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1
    val yearOk = year.toIntOrNull()?.let { it in MIN_OBD2_YEAR..maxYear } == true
    fun positive(s: String) = (s.toDoubleOrNull() ?: 0.0) > 0
    val valid = make.isNotBlank() && yearOk && positive(tank) && positive(mpg) && positive(price)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextPrimary,
        title = { Text(if (initial == null) "Add vehicle" else "Edit vehicle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field("Make", make, { make = it }, isError = make.isBlank())
                Field("Model", model, { model = it })
                Field("Year ($MIN_OBD2_YEAR–$maxYear)", year, { year = it }, KeyboardType.Number, !yearOk)
                Field("Tank size (gal)", tank, { tank = it }, KeyboardType.Decimal, !positive(tank))
                Field("Baseline MPG", mpg, { mpg = it }, KeyboardType.Decimal, !positive(mpg))
                Field("Fuel price ($/gal)", price, { price = it }, KeyboardType.Decimal, !positive(price))
                if (initial != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete vehicle", color = Amber) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    Vehicle(
                        id = initial?.id ?: 0,
                        make = make.trim(),
                        model = model.trim(),
                        year = year.toInt(),
                        tankGal = tank.toDouble(),
                        baselineMpg = mpg.toDouble(),
                        fuelPricePerGal = price.toDouble(),
                    )
                )
                onDismiss()
            }) { Text("Save", color = if (valid) ButtonGreen else TextSecondary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextPrimary) } },
    )

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = "Delete ${initial.displayName}?",
            body = "All trips logged for this vehicle are deleted too. This can't be undone.",
            confirmText = "Delete",
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}

private const val MIN_OBD2_YEAR = 1996 // OBD-II became mandatory in the US for model year 1996

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError && value.isNotEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = ecoFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun VehicleSelectorPreview() {
    VehicleSelectorContent(
        vehicles = listOf(
            Vehicle(1, "Honda", "Civic", 2014, 13.2, 33.0, 3.49),
            Vehicle(2, "Toyota", "Tacoma", 2019, 21.0, 20.0, 3.49),
        ),
        activeId = 1,
        onSelect = {}, onSave = {}, onDelete = {},
    )
}
