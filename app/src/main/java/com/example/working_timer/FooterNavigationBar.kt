package com.example.working_timer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
                    painter = painterResource(id = R.drawable.ic_timer),
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
                    painter = painterResource(id = R.drawable.ic_calender),
                    contentDescription = "履歴"
                )
            }
        )
    }
}
