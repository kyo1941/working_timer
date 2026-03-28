package com.example.working_timer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.working_timer.R
import com.example.working_timer.util.PauseButtonColor
import com.example.working_timer.util.ResumeButtonColor
import com.example.working_timer.util.StartButtonColor
import com.example.working_timer.util.StopButtonColor

@Composable
internal fun TimerControlButtons(
    timerStatus: TimerStatus?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (timerStatus) {
        TimerStatus.Working -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            TimerButton(
                text = stringResource(R.string.stop_timer_button_text),
                color = StopButtonColor,
                onClick = onStopTimer
            )

            Spacer(Modifier.weight(1f))

            TimerButton(
                text = stringResource(R.string.pause_timer_button_text),
                color = PauseButtonColor,
                onClick = onPauseTimer
            )

            Spacer(Modifier.weight(1f))
        }

        TimerStatus.Resting -> TimerButton(
            text = stringResource(R.string.resume_timer_button_text),
            color = ResumeButtonColor,
            onClick = onResumeTimer,
            modifier = modifier
        )

        null -> TimerButton(
            text = stringResource(R.string.start_timer_button_text),
            color = StartButtonColor,
            onClick = onStartTimer,
            modifier = modifier
        )
    }
}
