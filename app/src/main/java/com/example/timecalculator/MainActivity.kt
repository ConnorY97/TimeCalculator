// MainActivity.kt
package com.example.timecalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ArrayAdapter
import android.widget.ListView

class MainActivity : ComponentActivity() {

    private val intervals = mutableListOf<Pair<String, String>>()
    private val timeFormat = SimpleDateFormat("HHmm", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startTimeInput: EditText = findViewById(R.id.startTimeInput)
        val endTimeInput: EditText = findViewById(R.id.endTimeInput)
        val intervalIndexInput: EditText = findViewById(R.id.intervalIndexInput)
        val addButton: Button = findViewById(R.id.addButton)
        val editButton: Button = findViewById(R.id.editButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val resultTextView: TextView = findViewById(R.id.resultTextView)
        val intervalListView: ListView = findViewById(R.id.intervalListView)

        // Adapter for ListView
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        intervalListView.adapter = adapter

        addButton.setOnClickListener {
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()

            if (isValidTime(startTime) && isValidTime(endTime)) {
                intervals.add(Pair(startTime, endTime))
                adapter.add("Interval: $startTime - $endTime")
                val totalDuration = calculateTotalDuration()
                resultTextView.text = formatResult(totalDuration)
                startTimeInput.text.clear()
                endTimeInput.text.clear()
            } else {
                resultTextView.text = "Invalid time format. Use HHmm (e.g., 0845)."
            }
        }

        // Handle item selection for editing
        intervalListView.setOnItemClickListener { _, _, position, _ ->
            val selectedInterval = intervals[position]
            startTimeInput.setText(selectedInterval.first)
            endTimeInput.setText(selectedInterval.second)
            intervalIndexInput.setText(position.toString())
        }

        editButton.setOnClickListener {
            val indexText = intervalIndexInput.text.toString()
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()

            val index = indexText.toIntOrNull()
            if (index != null && index in intervals.indices) {
                if (isValidTime(startTime) && isValidTime(endTime)) {
                    // Update the selected interval in the list
                    intervals[index] = Pair(startTime, endTime)

                    // Update the ListView item
                    adapter.clear()  // Clear all items in the ListView
                    intervals.forEach { (start, end) ->  // Add all updated intervals back
                        adapter.add("Interval: $start - $end")
                    }

                    val totalDuration = calculateTotalDuration()
                    resultTextView.text = formatResult(totalDuration)
                    startTimeInput.text.clear()
                    endTimeInput.text.clear()
                    intervalIndexInput.text.clear()
                } else {
                    resultTextView.text = "Invalid time format. Use HHmm (e.g., 0845)."
                }
            } else {
                resultTextView.text = "Invalid index. Enter a valid interval number."
            }
        }

        clearButton.setOnClickListener {
            intervals.clear()
            adapter.clear()  // Clear the ListView
            resultTextView.text = ""
            startTimeInput.text.clear()
            endTimeInput.text.clear()
            intervalIndexInput.text.clear()
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

