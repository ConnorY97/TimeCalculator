// TimeIntervalAdapter.kt
package com.example.timecalculator

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView

class TimeIntervalAdapter(
    private val intervals: MutableList<TimeInterval>,
    private val onIntervalChanged: () -> Unit,
    private val onValidationError: (String) -> Unit,
    private val onSaveIntervals: () -> Unit
) : RecyclerView.Adapter<TimeIntervalAdapter.IntervalViewHolder>() {

    inner class IntervalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTimeEdit: EditText = view.findViewById(R.id.startTimeEdit)
        val endTimeEdit: EditText = view.findViewById(R.id.endTimeEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntervalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.interval_item, parent, false)
        return IntervalViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntervalViewHolder, position: Int) {
        val interval = intervals[position]

        // Set up edit values
        holder.startTimeEdit.setText(interval.startTime)
        holder.endTimeEdit.setText(interval.endTime)

        holder.startTimeEdit.setOnEditorActionListener { _, actionId, event ->
            return@setOnEditorActionListener setUpActionListener(
                holder.startTimeEdit,
                holder.endTimeEdit,
                actionId,
                event,
                position
            )
        }

        holder.endTimeEdit.setOnEditorActionListener { _, actionId, event ->
            return@setOnEditorActionListener setUpActionListener(
                holder.startTimeEdit,
                holder.endTimeEdit,
                actionId,
                event,
                position
            )
        }
    }

    private fun setUpActionListener(
        startEdit: EditText,
        endEdit: EditText,
        actionId: Int,
        event: KeyEvent?,
        position: Int
    ): Boolean {
        val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
        val isDoneAction = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE

        if (isEnterKey || isDoneAction) {
            updateValues(position, startEdit.text.toString(), endEdit.text.toString())
            return true
        }

        return false
    }


    private fun updateValues(position: Int, startVal: String, endVal: String) {
        val updated = TimeInterval(startVal, endVal)

        if (!updated.isValid()) {
            onValidationError("Invalid time format. Use 24-hour format (e.g., 0845)")
            return
        }

        intervals[position] = updated
        notifyItemChanged(position)
        onIntervalChanged()
        onSaveIntervals()
    }
    
    override fun getItemCount() = intervals.size
}
