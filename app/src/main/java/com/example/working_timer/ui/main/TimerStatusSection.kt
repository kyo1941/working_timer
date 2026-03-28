package com.example.working_timer.ui.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.working_timer.R
import com.example.working_timer.util.StatusPauseColor
import com.example.working_timer.util.StatusWorkingColor

@Composable
internal fun TimerStatusSection(timerStatus: TimerStatus?, modifier: Modifier = Modifier) {
    timerStatus?.let {
        Text(
            text = when (timerStatus) {
                TimerStatus.Working -> stringResource(R.string.working_status)
                TimerStatus.Resting -> stringResource(R.string.resting_status)
            },
            textAlign = TextAlign.Center,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = when (timerStatus) {
                TimerStatus.Working -> StatusWorkingColor
                TimerStatus.Resting -> StatusPauseColor
            },
            modifier = modifier
        )
    }
}
