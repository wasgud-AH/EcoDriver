package com.example.ecodriver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ecodriver.data.MenuItemData
import com.example.ecodriver.ui.components.GridMenuItem
import com.example.ecodriver.ui.navigation.Screens

@Composable
fun MainMenuScreen(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit) {


    val menuButtons = listOf(
        MenuItemData("Statistics", Screens.Statistics.route),
        MenuItemData("Dashboard",Screens.Dashboard.route),
        MenuItemData("Live Data",Screens.LiveData.route),
        MenuItemData("All Sensors",Screens.AllSensors.route),
        MenuItemData("Diagnostic Tools",Screens.DiagnosticTools.route),
        MenuItemData("Vehicle Selector",Screens.VehicleSelector.route),
        MenuItemData("Settings",Screens.Settings.route),
        MenuItemData("Data Import/Export",Screens.DataExport.route)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(menuButtons) {
                item -> GridMenuItem(
            item = item,
            onClick = {
                onNavigate(item.route)
            }
        )
        }
    }
}
