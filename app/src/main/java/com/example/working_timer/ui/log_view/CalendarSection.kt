package com.example.working_timer.ui.log_view

import android.view.LayoutInflater
import android.widget.CalendarView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.working_timer.R
import java.text.SimpleDateFormat

@Composable
internal fun CalendarSection(
    state: LogViewScreenState,
    actions: LogViewScreenActions,
    sdf: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.calender_view, null)
            val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                actions.onDateSelected(year, month, dayOfMonth)
            }
            view
        },
        update = { view ->
            val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
            val dateMillis =
                if (state.uiState.selectedDay.isNotEmpty()) sdf.parse(state.uiState.selectedDay)?.time else null
            if (dateMillis != null && calendarView != null) {
                calendarView.date = dateMillis
            }
        },
        modifier = modifier
    )
}
