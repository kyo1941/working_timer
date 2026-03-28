package com.example.working_timer.ui.main

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TimerButton(text: String, color: Color, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        shape = RoundedCornerShape(40.dp),
        modifier = Modifier.size(100.dp)
    ) {
        Text(text, fontSize = 20.sp, color = Color.White)
    }
}
