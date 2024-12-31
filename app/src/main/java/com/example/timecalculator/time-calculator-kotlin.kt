// TimeInterval.kt
package com.example.timecalculator

data class TimeInterval(
    var startTime: String,
    var endTime: String,
    var isEditing: Boolean = false
) {
    fun isValid(): Boolean {
        return startTime.length == 4 && endTime.length == 4 &&
                startTime.all { it.isDigit() } && endTime.all { it.isDigit() } &&
                startTime.toInt() in 0..2359 && endTime.toInt() in 0..2359 &&
                startTime.substring(2).toInt() < 60 && endTime.substring(2).toInt() < 60 &&
                startTime.toInt() < endTime.toInt()
    }

    fun overlaps(other: TimeInterval): Boolean {
        if (this === other) return false
        val start1 = startTime.toInt()
        val end1 = endTime.toInt()
        val start2 = other.startTime.toInt()
        val end2 = other.endTime.toInt()
        return !(end1 <= start2 || start1 >= end2)
    }
}
