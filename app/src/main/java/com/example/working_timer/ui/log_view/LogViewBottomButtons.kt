package com.example.working_timer.ui.log_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.working_timer.R
import com.example.working_timer.util.ButtonBackgroundColor

@Composable
internal fun LogViewBottomButtons(
    state: LogViewScreenState,
    actions: LogViewScreenActions,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = {
                actions.onNavigateToEditWork(
                    0,
                    state.uiState.selectedDay
                )
            },
            containerColor = ButtonBackgroundColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                modifier = Modifier
                    .height(24.dp)
                    .width(24.dp),
                contentDescription = stringResource(id = R.string.log_view_add_button_description)
            )
        }
        FloatingActionButton(
            onClick = actions.onShowDateRangePicker,
            containerColor = ButtonBackgroundColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CurrencyYen,
                contentDescription = stringResource(id = R.string.log_view_calculate_salary_button_description)
            )
        }
    }
}
