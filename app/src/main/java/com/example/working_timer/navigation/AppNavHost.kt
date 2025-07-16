package com.example.working_timer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.working_timer.ui.edit_work.EditWorkScreen
import com.example.working_timer.ui.log_view.LogViewScreen
import com.example.working_timer.ui.main.MainScreen

@Composable
fun AppNavHost (
    navController: NavHostController,
    startDestination: String = Routes.Timer.routes
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Timer.routes)  {
            MainScreen (
                onNavigateToLog = {
                    navController.navigate(Routes.LogView.routes) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.LogView.routes) {
            LogViewScreen(
                onNavigateToTimer = {
                    navController.navigate(Routes.Timer.routes) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditWork = { isNew, id, startDay, endDay, startTime, endTime, elapsedTime ->
                    navController.navigate(
                        Routes.EditWork().createRoute(
                            id = id,
                            isNew = isNew,
                            startDay = startDay,
                            endDay = endDay,
                            startTime = startTime,
                            endTime = endTime,
                            elapsedTime = elapsedTime
                        )
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.EditWork().routes,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("isNew") { type = NavType.BoolType },
                navArgument("startDay") { type = NavType.StringType },
                navArgument("endDay") { type = NavType.StringType },
                navArgument("startTime") { type = NavType.StringType },
                navArgument("endTime") { type = NavType.StringType },
                navArgument("elapsedTime") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val isNew = backStackEntry.arguments?.getBoolean("isNew") ?: true
            val startDay = backStackEntry.arguments?.getString("startDay") ?: ""
            val endDay = backStackEntry.arguments?.getString("endDay") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: "00:00"
            val endTime = backStackEntry.arguments?.getString("endTime") ?: "00:00"
            val elapsedTime = backStackEntry.arguments?.getInt("elapsedTime") ?: 0

            EditWorkScreen(
                id = id,
                isNew = isNew,
                startDay = startDay,
                endDay = endDay,
                startTime = startTime,
                endTime = endTime,
                elapsedTime = elapsedTime,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}