package com.example.working_timer.ui.log_view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.working_timer.ui.components.WorkItemComposable
import com.example.working_timer.util.BorderColor
import com.example.working_timer.util.ButtonBackgroundColor

@Composable
internal fun WorkListSection(
    state: LogViewScreenState,
    actions: LogViewScreenActions,
    modifier: Modifier = Modifier
) {
    if (state.uiState.isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = ButtonBackgroundColor
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            itemsIndexed(state.uiState.workList) { index, work ->
                WorkItemComposable(
                    work = work,
                    onDelete = { actions.onShowDeleteDialog(work) },
                    onEdit = {
                        actions.onNavigateToEditWork(
                            work.id,
                            work.start_day
                        )
                    }
                )
                if (index < state.uiState.workList.lastIndex) {
                    HorizontalDivider(
                        color = BorderColor,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}
