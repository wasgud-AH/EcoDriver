package com.example.ecodriver.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ecodriver.ui.screens.MainMenuScreen
import androidx.compose.ui.Modifier
import com.example.ecodriver.ui.components.ConnectionStatus

sealed class Screens(val route: String){
    object MainMenu : Screens("main_menu")
    object Statistics : Screens("statistics")
    object Dashboard : Screens("dashboard")
    object LiveData : Screens("live_data")
    object AllSensors : Screens("all_sensors")
    object DiagnosticTools : Screens("diagnostic_tools")
    object VehicleSelector : Screens("vehicle_selector")
    object Settings : Screens("settings")
    object DataExport : Screens("data_export")
}

@Composable
fun EcoDriverNavHost(
    modifier: Modifier = Modifier,
    navHostController: NavHostController = rememberNavController()
){
    NavHost(
        navHostController,
        startDestination = Screens.MainMenu.route,
        modifier = modifier
    ){

    /*
    This section defines our screens, stemming from MainMenuGrid
     */
        composable(Screens.MainMenu.route){
            MainMenuScreen(
                onNavigate = {route -> navHostController.navigate(route)}
            )
            ConnectionStatus()
        }
    }
}