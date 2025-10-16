package com.example.working_timer.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.working_timer.navigation.AppNavHost
import com.example.working_timer.navigation.Routes
import com.example.working_timer.ui.components.FooterNavigationBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val navBackStackEntry = navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry.value?.destination
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost(
                        navController = navController,
                        footer = {
                            FooterNavigationBar(
                                selectedIndex = when (currentDestination?.route) {
                                    Routes.Timer.routes -> 0
                                    Routes.LogView.routes -> 1
                                    else -> -1
                                },
                                onTimerClick = {
                                    navController.navigate(Routes.Timer.routes) {
                                        launchSingleTop = true
                                    }
                                },
                                onLogClick = {
                                    navController.navigate(Routes.LogView.routes) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
