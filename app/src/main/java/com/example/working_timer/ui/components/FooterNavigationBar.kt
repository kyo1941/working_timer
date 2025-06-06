package com.example.working_timer.ui.components

import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.working_timer.R

@Composable
fun FooterNavigationBar(
    selectedIndex: Int,
    onTimerClick: () -> Unit,
    onLogClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            label = { Text("記録") },
            onClick = onTimerClick,
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.AccessTime,
                    contentDescription = "記録"
                )
            }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            label = { Text("履歴") },
            onClick = onLogClick,
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.CalendarMonth,
                    contentDescription = "履歴"
                )
            }
        )
    }
}
