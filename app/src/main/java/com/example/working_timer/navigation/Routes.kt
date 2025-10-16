package com.example.working_timer.navigation

sealed class Routes(val routes: String) {
    object Timer : Routes("timer")
    object LogView : Routes("log_view")
    data class EditWork(
        val id: Int = 0,
        val startDay: String = "",
        val isNew: Boolean = true
    ) : Routes("edit_work/{id}/{startDay}/{isNew}") {
        fun createRoute(
            id: Int = 0,
            startDay: String = "",
            isNew: Boolean = true
        ): String {
            return "edit_work/$id/$startDay/$isNew"
        }
    }

    object TimerDeepLink : Routes("app://working_timer/timer")
}