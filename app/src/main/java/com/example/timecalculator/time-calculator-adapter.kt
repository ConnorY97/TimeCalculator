// TimeIntervalAdapter.kt
package com.example.timecalculator

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimeIntervalAdapter(
    private val intervals: MutableList<TimeInterval>,
    private val onIntervalChanged: () -> Unit,
    private val onValidationError: (String) -> Unit
) : RecyclerView.Adapter<TimeIntervalAdapter.IntervalViewHolder>() {

    inner class IntervalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTimeText: TextView = view.findViewById(R.id.startTimeText)
        val endTimeText: TextView = view.findViewById(R.id.endTimeText)
        val startTimeEdit: EditText = view.findViewById(R.id.startTimeEdit)
        val endTimeEdit: EditText = view.findViewById(R.id.endTimeEdit)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val saveButton: Button = view.findViewById(R.id.saveButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntervalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.interval_item, parent, false)
        return IntervalViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntervalViewHolder, position: Int) {
        val interval = intervals[position]
        
        // Set up display values
        holder.startTimeText.text = interval.startTime
        holder.endTimeText.text = interval.endTime

        // Set up edit values
        holder.startTimeEdit.setText(interval.startTime)
        holder.endTimeEdit.setText(interval.endTime)

        // Configure visibility based on edit mode
        updateViewVisibility(holder, interval.isEditing)

        // Set up button click listeners
        holder.editButton.setOnClickListener {
            interval.isEditing = true
            updateViewVisibility(holder, true)
        }

        holder.deleteButton.setOnClickListener {
            intervals.removeAt(position)
            notifyItemRemoved(position)
            onIntervalChanged()
        }


        holder.saveButton.setOnClickListener {
            val newStart = holder.startTimeEdit.text.toString()
            val newEnd = holder.endTimeEdit.text.toString()
            val newInterval = TimeInterval(newStart, newEnd)

            if (!newInterval.isValid()) {
                onValidationError("Invalid time format. Use 24-hour format (e.g., 0845)")
                return@setOnClickListener
            }

            // Check for overlaps with other intervals
            val otherIntervals = intervals.filterIndexed { index, _ -> index != position }
            if (otherIntervals.any { newInterval.overlaps(it) }) {
                onValidationError("This interval overlaps with an existing interval")
                return@setOnClickListener
            }

            interval.startTime = newStart
            interval.endTime = newEnd
            interval.isEditing = false

            updateViewVisibility(holder, false)

            // Notify adapter about the change
            notifyItemChanged(position)

            onIntervalChanged()
        }


        holder.cancelButton.setOnClickListener {
            interval.isEditing = false
            holder.startTimeEdit.setText(interval.startTime)
            holder.endTimeEdit.setText(interval.endTime)
            updateViewVisibility(holder, false)
        }

        // Set up text change listeners for validation
        setupEditTextValidation(holder.startTimeEdit)
        setupEditTextValidation(holder.endTimeEdit)
    }

    private fun updateViewVisibility(holder: IntervalViewHolder, isEditing: Boolean) {
        holder.startTimeText.visibility = if (isEditing) View.GONE else View.VISIBLE
        holder.endTimeText.visibility = if (isEditing) View.GONE else View.VISIBLE
        holder.startTimeEdit.visibility = if (isEditing) View.VISIBLE else View.GONE
        holder.endTimeEdit.visibility = if (isEditing) View.VISIBLE else View.GONE
        holder.editButton.visibility = if (isEditing) View.GONE else View.VISIBLE
        holder.deleteButton.visibility = if (isEditing) View.GONE else View.VISIBLE
        holder.saveButton.visibility = if (isEditing) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (isEditing) View.VISIBLE else View.GONE
    }

    private fun setupEditTextValidation(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) > 4) {
                    editText.setText(s?.subSequence(0, 4))
                    editText.setSelection(4)
                }
            }
        })
    }

    override fun getItemCount() = intervals.size
}
