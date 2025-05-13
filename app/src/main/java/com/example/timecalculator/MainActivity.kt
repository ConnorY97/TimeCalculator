// MainActivity.kt
package com.example.timecalculator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    private val intervals = mutableListOf<TimeInterval>()
    private val timeFormat = SimpleDateFormat("HHmm", Locale.US)
    private lateinit var adapter: TimeIntervalAdapter
    private val gson = Gson()
    private val sharedPrefs by lazy { getSharedPreferences("time_intervals", Context.MODE_PRIVATE) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startTimeInput: EditText = findViewById(R.id.startTimeInput)
        val endTimeInput: EditText = findViewById(R.id.endTimeInput)
        val addButton: Button = findViewById(R.id.addButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val totalDurationText: TextView = findViewById(R.id.totalDurationText)
        val recyclerView: RecyclerView = findViewById(R.id.intervalsRecyclerView)
        val currentTimeButton: Button = findViewById(R.id.currentTimeButton)

        // Set the start time input as the focus and open the keyboard on launch
        startTimeInput.requestFocus()
        startTimeInput.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(startTimeInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)

        // Set the end time input as the focus if enter in pressed while in the start time input
        startTimeInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            val isDone = (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE)

            if (isEnter || isDone) {
                endTimeInput.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // Add interval if enter is pressed while in end time input
        endTimeInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            val isDone = (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE)

            if (isEnter || isDone) {
                addInterval(startTimeInput, endTimeInput, totalDurationText)
                startTimeInput.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // Set up RecyclerView
        adapter = TimeIntervalAdapter(
            intervals = intervals,
            onIntervalChanged = { updateTotalDuration(totalDurationText) },
            onValidationError = { message -> showError(message) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Safe to load intervals
        // Load any existing intervals
        loadIntervals()

        addButton.setOnClickListener {
            addInterval(startTimeInput, endTimeInput, totalDurationText)
        }

        clearButton.setOnClickListener {
            val size = intervals.size
            intervals.clear()
            adapter.notifyItemRangeRemoved(0, size)  // Specify the range of items removed
            updateTotalDuration(totalDurationText)
            startTimeInput.text.clear()
            endTimeInput.text.clear()

            // Rest the focus on the start time
            startTimeInput.requestFocus()
        }

        currentTimeButton.setOnClickListener {
            val currentTime = timeFormat.format(Calendar.getInstance().time)

            when {
                startTimeInput.isFocused -> startTimeInput.setText(currentTime)
                endTimeInput.isFocused -> endTimeInput.setText(currentTime)
                else -> showError("Please select a field to input the current time.")
            }
        }
    }

    private fun addInterval(
        startTimeInput: EditText,
        endTimeInput: EditText,
        totalDurationText: TextView
    ) {
        val startTime = startTimeInput.text.toString()
        val endTime = endTimeInput.text.toString()

        val newInterval = TimeInterval(startTime, endTime)

        when {
            !newInterval.isValid() -> {
                showError("Invalid time format. Use 24-hour format (e.g., 0845)")
            }

            intervals.any { newInterval.overlaps(it) } -> {
                showError("This interval overlaps with an existing interval")
            }

            else -> {
                intervals.add(newInterval)
                adapter.notifyItemInserted(intervals.size - 1)
                updateTotalDuration(totalDurationText)
                startTimeInput.text.clear()
                endTimeInput.text.clear()
            }
        }

        // Rest the focus on the start time
        startTimeInput.requestFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        Log.d("MainActivity", "Menu created")  // Check Logcat to confirm this runs
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save -> {
                saveIntervals()
                Log.i("menu_save", "Saving intervals")
                true
            }
            R.id.menu_main -> {
                val homeScreen = Intent(this, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(homeScreen)
                finish() // Optional if you want to finish this activity explicitly
                true
            }
            R.id.menu_exit -> {
                // Handle Exit action
                finishAffinity() // Close the app
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTotalDuration(totalDurationText: TextView) {
        var totalMinutes = 0L

        for (interval in intervals) {
            val startTime = timeFormat.parse(interval.startTime)!!
            val endTime = timeFormat.parse(interval.endTime)!!
            val duration = (endTime.time - startTime.time) / (1000 * 60)
            totalMinutes += duration
        }

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        // Use the string resource with placeholders
        val formattedTotal = getString(R.string.total_duration, hours, minutes)
        totalDurationText.text = formattedTotal

    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveIntervals() {
        val json = gson.toJson(intervals)
        sharedPrefs.edit() { putString("interval_list", json) }
        Toast.makeText(this, "Intervals saved", Toast.LENGTH_SHORT).show()
    }

    private fun loadIntervals() {
        val json = sharedPrefs.getString("interval_list", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<TimeInterval>>() {}.type
            val savedIntervals: MutableList<TimeInterval> = gson.fromJson(json, type)

            intervals.clear()
            adapter.notifyItemRangeRemoved(0, adapter.itemCount)  // Let RecyclerView know items were removed

            intervals.addAll(savedIntervals)
            adapter.notifyItemRangeInserted(0, intervals.size)    // Let RecyclerView know new items were inserted
        }
    }

}
