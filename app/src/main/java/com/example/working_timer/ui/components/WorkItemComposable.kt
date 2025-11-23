package com.example.working_timer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.working_timer.R
import com.example.working_timer.data.db.Work

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
                    stringResource(id = R.string.work_item_activity_time_label),
                    modifier = Modifier.padding(end = 16.dp),
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
                        stringResource(id = R.string.work_item_hour_unit),
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
                    stringResource(id = R.string.work_item_minute_unit),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.work_item_start_time_label, work.start_time),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(id = R.string.work_item_end_time_label, work.end_time),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.work_item_edit_button_description),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                contentDescription = stringResource(id = R.string.work_item_delete_button_description)
            )
        }
    }
}
