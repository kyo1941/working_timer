package com.example.working_timer.navigation

import android.util.Log

sealed class Routes(val routes: String) {
    object Timer: Routes("timer")
    object LogView: Routes("log_view")
    data class EditWork(
        val id: Int = 0,
        val isNew: Boolean = true,
        val startDay: String = "",
        val endDay: String = "",
        val startTime: String = "00:00",
        val endTime: String = "00:00",
        val elapsedTime: Int = 0
    ) : Routes("edit_work/{id}/{isNew}/{startDay}/{endDay}/{startTime}/{endTime}/{elapsedTime}") {
        fun createRoute(
            id: Int = 0,
            isNew: Boolean = true,
            startDay: String = "",
            endDay: String = "",
            startTime: String = "00:00",
            endTime: String = "00:00",
            elapsedTime: Int = 0
        ) : String {
            return "edit_work/$id/$isNew/$startDay/$endDay/$startTime/$endTime/$elapsedTime"
        }
    }
}