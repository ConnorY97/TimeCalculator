package com.example.timecalculator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

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
        val totalValueText: TextView = findViewById(R.id.totalValueText)
        val recyclerView: RecyclerView = findViewById(R.id.intervalsRecyclerView)
        val currentTimeButton: Button = findViewById(R.id.currentTimeButton)

        startTimeInput.requestFocus()
        startTimeInput.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(startTimeInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)

        startTimeInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            val isDone = (actionId == EditorInfo.IME_ACTION_DONE)
            if (isEnter || isDone) {
                endTimeInput.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        endTimeInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            val isDone = (actionId == EditorInfo.IME_ACTION_DONE)
            if (isEnter || isDone) {
                addInterval(startTimeInput, endTimeInput, totalDurationText, totalValueText)
                startTimeInput.requestFocus()
                saveIntervals()
                return@setOnEditorActionListener true
            }
            false
        }

        adapter = TimeIntervalAdapter(
            intervals = intervals,
            onIntervalChanged = {
                updateTotalDuration(totalDurationText)
                updateTotalValue(totalValueText)
            },
            onValidationError = { message -> showError(message) },
            onSaveIntervals = { saveIntervals() }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadIntervals()
        updateTotalDuration(totalDurationText)
        updateTotalValue(totalValueText)

        addButton.setOnClickListener {
            addInterval(startTimeInput, endTimeInput, totalDurationText, totalValueText)
            saveIntervals()
        }

        clearButton.setOnClickListener {
            val size = intervals.size
            intervals.clear()
            adapter.notifyItemRangeRemoved(0, size)
            updateTotalDuration(totalDurationText)
            updateTotalValue(totalValueText)
            startTimeInput.text.clear()
            endTimeInput.text.clear()
            startTimeInput.requestFocus()
            saveIntervals()
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
        totalDurationText: TextView,
        totalValueText: TextView
    ) {
        val startTime = startTimeInput.text.toString()
        val endTime = endTimeInput.text.toString()
        val newInterval = TimeInterval(startTime, endTime)

        when {
            !newInterval.isValid() -> showError("Invalid time format. Use 24-hour format (e.g., 0845)")
            intervals.any { newInterval.overlaps(it) } -> showError("This interval overlaps with an existing interval")
            else -> {
                intervals.add(newInterval)
                adapter.notifyItemInserted(intervals.size - 1)
                updateTotalDuration(totalDurationText)
                updateTotalValue(totalValueText)
                startTimeInput.text.clear()
                endTimeInput.text.clear()
            }
        }

        startTimeInput.requestFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save -> {
                saveIntervals()
                true
            }
            R.id.menu_main -> {
                val homeScreen = Intent(this, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(homeScreen)
                finish()
                true
            }
            R.id.menu_exit -> {
                finishAffinity()
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
        val formattedTotal = getString(R.string.total_duration, hours, minutes)
        totalDurationText.text = formattedTotal
    }

    private fun updateTotalValue(totalValueText: TextView) {
        var totalMinutes = 0L
        for (interval in intervals) {
            val startTime = timeFormat.parse(interval.startTime)!!
            val endTime = timeFormat.parse(interval.endTime)!!
            val duration = (endTime.time - startTime.time) / (1000 * 60)
            totalMinutes += duration
        }
        val decimalHours = totalMinutes / 60.0
        val formattedValue = String.format(Locale.ENGLISH, "%.2f", decimalHours)
        totalValueText.text = getString(R.string.total_value, formattedValue)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveIntervals() {
        val json = gson.toJson(intervals)
        sharedPrefs.edit { putString("interval_list", json) }
    }

    private fun loadIntervals() {
        val json = sharedPrefs.getString("interval_list", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<TimeInterval>>() {}.type
            val savedIntervals: MutableList<TimeInterval> = gson.fromJson(json, type)
            intervals.clear()
            adapter.notifyItemRangeRemoved(0, adapter.itemCount)
            intervals.addAll(savedIntervals)
            adapter.notifyItemRangeInserted(0, intervals.size)
        }
    }
}
