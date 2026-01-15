package com.example.working_timer.navigation

sealed class Routes(val routes: String) {
    object Timer : Routes("timer")
    object LogView : Routes("log_view")
    object EditWork : Routes("edit_work/{id}/{startDay}") {
        fun createRoute(
            id: Int,
            startDay: String,
        ): String {
            return "edit_work/$id/$startDay"
        }
    }

    object TimerDeepLink : Routes("app://working_timer/timer")
}