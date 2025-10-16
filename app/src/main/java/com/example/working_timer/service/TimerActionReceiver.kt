package com.example.working_timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, TimerService::class.java)

        when (action) {
            "ACTION_PAUSE_TIMER" -> {
                serviceIntent.putExtra("action", "pause")
            }

            "ACTION_RESUME_TIMER" -> {
                serviceIntent.putExtra("action", "resume")
            }
        }

        context.startService(serviceIntent)
    }
}