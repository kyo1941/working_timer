package com.example.working_timer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.transition.TransitionValues
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            elapsedTime = System.currentTimeMillis() - startTime
            updateTimerText()
            handler.postDelayed(this, 1000) // 1秒ごとに更新
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        pauseButton = findViewById(R.id.pauseButton)
        resumeButton = findViewById(R.id.resumeButton)

        startButton.setOnClickListener {
            startTimer()
        }
        stopButton.setOnClickListener {
            stopTimer()
        }
        pauseButton.setOnClickListener {
            pauseTimer()
        }
        resumeButton.setOnClickListener {
            resumeTimer()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.navigation_timer
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if(item.itemId != bottomNavigationView.selectedItemId) {
                when (item.itemId) {
                    R.id.navigation_timer -> {
                        // 現在の画面なので何もしない
                        true
                    }

                    R.id.navigation_log -> {
                        val intent = Intent(this, LogViewActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }

                    else -> false
                }
            } else {
                true
            }
        }
    }


    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isRunning = true
        handler.postDelayed(runnable, 0)
        updateStatusAndButtons("労働中")
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false
        elapsedTime = 0
        updateTimerText()
        updateStatusAndButtons("")
    }

    private fun pauseTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false
        updateStatusAndButtons("休憩中")
    }

    private fun resumeTimer() {
        startTime = System.currentTimeMillis() - elapsedTime
        isRunning = true
        handler.postDelayed(runnable, 0)
        updateStatusAndButtons("労働中")
    }

    private fun updateTimerText() {
        val minutes = (elapsedTime / 1000 / 60).toInt()
        val seconds = (elapsedTime / 1000 % 60).toInt()
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = formattedTime
    }

    private fun updateStatusAndButtons(status: String) {
        statusTextView.text = status

        when (status) {
            "労働中" -> statusTextView.setTextColor(ContextCompat.getColor(this, R.color.start_button_color))
            "休憩中" -> statusTextView.setTextColor(ContextCompat.getColor(this, R.color.pause_button_color))
            else -> statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }

        startButton.visibility = if (status == "") Button.VISIBLE else Button.GONE
        stopButton.visibility = if (status == "労働中") Button.VISIBLE else Button.GONE
        pauseButton.visibility = if (status == "労働中") Button.VISIBLE else Button.GONE
        resumeButton.visibility = if (status == "休憩中") Button.VISIBLE else Button.GONE
    }
}