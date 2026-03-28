package com.example.working_timer.ui.edit_work

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.working_timer.R

@Composable
internal fun ElapsedSection(
    state: EditWorkScreenState,
    actions: EditWorkScreenActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.edit_work_screen_elapsed_time_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = actions.onShowElapsedPicker) {
                Text(
                    text = buildAnnotatedString {
                        if (state.uiState.elapsedHour > 0) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(state.uiState.elapsedHour.toString())
                            }
                            append(stringResource(id = R.string.edit_work_screen_hour_unit))
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(state.uiState.elapsedMinute.toString())
                        }
                        append(stringResource(id = R.string.edit_work_screen_minute_unit))
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
