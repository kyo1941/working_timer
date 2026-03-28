package com.example.working_timer.ui.edit_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.working_timer.R

@Composable
internal fun EditWorkActionButtons(
    actions: EditWorkScreenActions,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = actions.onNavigateBack,
            modifier = Modifier
                .width(120.dp)
                .height(56.dp)
        ) {
            Text(stringResource(id = R.string.edit_work_screen_cancel_button))
        }
        Spacer(modifier = Modifier.width(64.dp))
        Button(
            onClick = { actions.onSaveWork(false) },
            modifier = Modifier
                .width(120.dp)
                .height(56.dp)
        ) {
            Text(stringResource(id = R.string.edit_work_screen_save_button))
        }
    }
}
