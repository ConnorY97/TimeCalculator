// MainActivity.kt
package com.example.timecalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val intervals = mutableListOf<Pair<String, String>>()
    private val timeFormat = SimpleDateFormat("HHmm", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startTimeInput: EditText = findViewById(R.id.startTimeInput)
        val endTimeInput: EditText = findViewById(R.id.endTimeInput)
        val addButton: Button = findViewById(R.id.addButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val resultTextView: TextView = findViewById(R.id.resultTextView)

        addButton.setOnClickListener {
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()

            if (isValidTime(startTime) && isValidTime(endTime)) {
                intervals.add(Pair(startTime, endTime))
                val totalDuration = calculateTotalDuration()
                resultTextView.text = formatResult(totalDuration)
                startTimeInput.text.clear()
                endTimeInput.text.clear()
            } else {
                resultTextView.text = "Invalid time format. Use HHmm (e.g., 0845)."
            }
        }

        clearButton.setOnClickListener {
            intervals.clear()
            resultTextView.text = ""
            startTimeInput.text.clear()
            endTimeInput.text.clear()
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            if (time.length != 4) throw IllegalArgumentException("Invalid length")
            timeFormat.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateTotalDuration(): Long {
        var totalMinutes = 0L
        for ((start, end) in intervals) {
            val startTime = timeFormat.parse(start)!!
            val endTime = timeFormat.parse(end)!!
            val duration = (endTime.time - startTime.time) / (1000 * 60)
            totalMinutes += duration
        }
        return totalMinutes
    }

    private fun formatResult(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val intervalDetails = intervals.joinToString("\n") { (start, end) ->
            val startTime = timeFormat.parse(start)!!
            val endTime = timeFormat.parse(end)!!
            val duration = (endTime.time - startTime.time) / (1000 * 60)
            "Interval: $start - $end (${duration / 60} hrs ${duration % 60} mins)"
        }
        return "Intervals:\n$intervalDetails\n\nTotal: $hours hrs $minutes mins"
    }
}
