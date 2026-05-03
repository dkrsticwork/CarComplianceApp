package com.carcomplianceapp.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.carcomplianceapp.ui.screens.addcar.AddCarScreen
import com.carcomplianceapp.ui.screens.apikey.ApiKeyScreen
import com.carcomplianceapp.ui.screens.main.MainScreen
import com.carcomplianceapp.ui.screens.onboarding.WelcomeScreen

object Routes {
    const val WELCOME   = "welcome"
    const val API_KEY   = "api_key"
    const val ADD_CAR   = "add_car"
    const val MAIN      = "main"
}

@Composable
fun AppNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.API_KEY) },
                onSkipToDemo = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.API_KEY) {
            ApiKeyScreen(
                onContinue = {
                    navController.navigate(Routes.ADD_CAR) {
                        popUpTo(Routes.API_KEY) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ADD_CAR + "?carId={carId}",
            arguments = listOf(navArgument("carId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments?.getLong("carId") ?: -1L
            AddCarScreen(
                editCarId = if (carId == -1L) null else carId,
                onCarSaved = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onAddCar = { navController.navigate(Routes.ADD_CAR) },
                onEditCar = { carId -> navController.navigate("${Routes.ADD_CAR}?carId=$carId") },
                onGoToApiKey = { navController.navigate(Routes.API_KEY) }
            )
        }
    }
}
