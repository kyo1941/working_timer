package com.example.working_timer.ui.components

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.working_timer.data.Work

@Composable
fun WorkItemComposable(
    work: Work,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val hours = work.elapsed_time / 3600
            val minutes = (work.elapsed_time % 3600) / 60
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "活動時間    ",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                )
                if (hours > 0) {
                    Text(
                        hours.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
                    Text(
                        "時間 ",
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize
                    )
                }
                Text(
                    minutes.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                )
                Text(
                    "分",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "開始  ${work.start_time}",
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "終了  ${work.end_time}",
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_edit),
                contentDescription = "編集",
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_delete),
                contentDescription = "削除"
            )
        }
    }
}
