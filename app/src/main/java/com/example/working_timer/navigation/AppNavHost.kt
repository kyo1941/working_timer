package com.example.working_timer.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
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
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Routes.Timer.routes,
    footer: @Composable () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            Routes.Timer.routes,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.TimerDeepLink.routes })
        ) {
            ScaffoldWithEdgeToEdge(
                footer = footer
            ) { paddingValues ->
                MainScreenHolder(
                    onNavigateToLog = {
                        navController.navigate(Routes.LogView.routes) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(Routes.LogView.routes) {
            ScaffoldWithEdgeToEdge(
                footer = footer
            ) { paddingValues ->
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
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

            ScaffoldWithEdgeToEdge(
                footer = { /* 編集画面ではフッターを表示しない */ }
            ) { paddingValues ->
                EditWorkScreenHolder(
                    id = id,
                    startDay = startDay,
                    isNew = isNew,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ScaffoldWithEdgeToEdge(
    footer: @Composable () -> Unit,
    content: @Composable (padding: PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = footer,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        content(paddingValues)
    }
}