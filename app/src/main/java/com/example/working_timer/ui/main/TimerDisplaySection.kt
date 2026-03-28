package com.example.working_timer.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
internal fun TimerDisplaySection(displayText: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val timerAnimation: AnimatedContentTransitionScope<Char>.() -> ContentTransform = {
            slideInVertically { height -> height } togetherWith
                    slideOutVertically { height -> -height }
        }
        displayText.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = timerAnimation,
                label = "TimerCharAnimation"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    textAlign = TextAlign.Center,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
