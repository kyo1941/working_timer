package com.example.working_timer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.working_timer.ui.edit_work.EditWorkScreenHolder
import com.example.working_timer.ui.log_view.LogViewScreenHolder
import com.example.working_timer.ui.main.MainScreenHolder

@Composable
fun AppNavHost (
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Routes.Timer.routes
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            Routes.Timer.routes,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.TimerDeepLink.routes })
        ) {
            MainScreenHolder (
                onNavigateToLog = {
                    navController.navigate(Routes.LogView.routes) {
                        launchSingleTop = true
                    }
                },
                modifier = modifier
            )
        }

        composable(Routes.LogView.routes) {
            LogViewScreenHolder(
                onNavigateToTimer = {
                    navController.navigate(Routes.Timer.routes) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditWork = { id, startDay, isNew ->
                    navController.navigate(
                        Routes.EditWork().createRoute(
                            id = id,
                            startDay = startDay,
                            isNew = isNew
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                modifier = modifier
            )
        }

        composable(
            route = Routes.EditWork().routes,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("startDay") { type = NavType.StringType },
                navArgument("isNew") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val startDay = backStackEntry.arguments?.getString("startDay") ?: ""
            val isNew = backStackEntry.arguments?.getBoolean("isNew") ?: true

            EditWorkScreenHolder(
                id = id,
                startDay = startDay,
                isNew = isNew,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}