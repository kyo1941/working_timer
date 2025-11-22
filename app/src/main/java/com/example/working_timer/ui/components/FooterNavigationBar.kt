package com.example.working_timer.ui.components

import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
            label = { Text(stringResource(id = R.string.footer_navigation_bar_record_label)) },
            onClick = onTimerClick,
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.AccessTime,
                    contentDescription = stringResource(id = R.string.footer_navigation_bar_record_content_description)
                )
            }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            label = { Text(stringResource(id = R.string.footer_navigation_bar_history_label)) },
            onClick = onLogClick,
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.CalendarMonth,
                    contentDescription = stringResource(id = R.string.footer_navigation_bar_history_content_description)
                )
            }
        )
    }
}
