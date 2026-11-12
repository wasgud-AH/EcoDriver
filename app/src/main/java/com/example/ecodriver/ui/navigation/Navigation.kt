package com.example.ecodriver.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ecodriver.ui.screens.MainMenuScreen
import com.example.ecodriver.ui.screens.SettingsScreen
import androidx.compose.ui.Modifier
import com.example.ecodriver.ui.screens.AllSensorsScreen
import com.example.ecodriver.ui.screens.DashboardScreen
import com.example.ecodriver.ui.screens.DataManagementScreen
import com.example.ecodriver.ui.screens.DiagnosticToolsScreen
import com.example.ecodriver.ui.screens.LiveDataScreen
import com.example.ecodriver.ui.screens.StatisticsScreen
import com.example.ecodriver.ui.screens.TripReportScreen
import com.example.ecodriver.ui.screens.VehicleSelectorScreen
import com.example.ecodriver.ui.ObdViewModel

sealed class Screens(val route: String){
    object MainMenu : Screens("main_menu")
    object Statistics : Screens("statistics")
    object Dashboard : Screens("dashboard")
    object LiveData : Screens("live_data")
    object AllSensors : Screens("all_sensors")
    object DiagnosticTools : Screens("diagnostic_tools")
    object VehicleSelector : Screens("vehicle_selector")
    object Settings : Screens("settings")

    object DataManagement : Screens("data_management")
    object TripReport : Screens("trip_report/{tripId}") {
        fun route(tripId: Long) = "trip_report/$tripId"
    }
}

@Composable
fun EcoDriverNavHost(
    obdViewModel: ObdViewModel,
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
                obdViewModel,
                onNavigate = {route -> navHostController.navigate(route)}
            )
        }

        composable(Screens.Statistics.route) {
            StatisticsScreen(onOpenTrip = { id -> navHostController.navigate(Screens.TripReport.route(id)) })
        }
        composable(
            Screens.TripReport.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
        ) { entry -> TripReportScreen(entry.arguments?.getLong("tripId") ?: 0L) }
        composable(Screens.Dashboard.route) { DashboardScreen() }
        composable(Screens.LiveData.route) { LiveDataScreen() }
        composable(Screens.AllSensors.route) { AllSensorsScreen() }
        composable(Screens.DiagnosticTools.route) { DiagnosticToolsScreen() }
        composable(Screens.VehicleSelector.route) { VehicleSelectorScreen() }
        composable(Screens.Settings.route) { SettingsScreen(obdViewModel) }
        composable(Screens.DataManagement.route) { DataManagementScreen() }


    }
}