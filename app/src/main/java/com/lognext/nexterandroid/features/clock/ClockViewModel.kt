package com.lognext.nexterandroid.features.clock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ClockViewModel : ViewModel() {
    var isCheckedIn by mutableStateOf(false)
        private set
    var currentTime by mutableStateOf(Date())
        private set
    var currentClockDay by mutableStateOf(dayString(Date()))
        private set
    var clockInTime by mutableStateOf<Date?>(null)
        private set
    var clockOutTime by mutableStateOf<Date?>(null)
        private set
    var todayEntries by mutableStateOf<List<ClockEntry>>(emptyList())
        private set
    var weekEntries by mutableStateOf<List<ClockEntry>>(sampleWeekEntries())
        private set
    var isLoadingStatus by mutableStateOf(false)
        private set
    var hasLoadedTodayEntries by mutableStateOf(true)
        private set
    var isSubmittingAction by mutableStateOf(false)
        private set
    var submittingClockIn by mutableStateOf(false)
        private set

    fun tick(date: Date) {
        currentTime = date
        val day = dayString(date)
        if (day != currentClockDay) {
            currentClockDay = day
            resetForNewDay()
        }
    }

    fun handleClockButton() {
        if (isSubmittingAction) return
        if (isCheckedIn) clockOut() else clockIn()
    }

    fun workedSeconds(entry: ClockEntry): Long {
        val clockIn = entry.clockIn ?: return 0L
        val effectiveClockOut = entry.clockOut ?: if (isCheckedIn) currentTime else null
        return maxOf(0L, ((effectiveClockOut?.time ?: return 0L) - clockIn.time) / 1000L)
    }

    private fun clockIn() {
        val now = currentTime
        val entry = ClockEntry(clockIn = now, clockOut = null)
        submittingClockIn = true
        isSubmittingAction = true
        clockInTime = now
        clockOutTime = null
        isCheckedIn = true
        todayEntries = todayEntries + entry
        weekEntries = upsertWeekEntry(entry)
        isSubmittingAction = false
        submittingClockIn = false
    }

    private fun clockOut() {
        val now = currentTime
        submittingClockIn = false
        isSubmittingAction = true
        val openEntry = todayEntries.lastOrNull { it.clockIn != null && it.clockOut == null }
        val updatedEntry = if (openEntry != null) {
            openEntry.copy(clockOut = now)
        } else {
            ClockEntry(clockIn = clockInTime, clockOut = now)
        }

        clockOutTime = now
        isCheckedIn = false
        todayEntries = todayEntries.map { if (it.recordId == updatedEntry.recordId) updatedEntry else it }
            .ifEmpty { listOf(updatedEntry) }
        weekEntries = upsertWeekEntry(updatedEntry)
        isSubmittingAction = false
    }

    private fun resetForNewDay() {
        isCheckedIn = false
        clockInTime = null
        clockOutTime = null
        todayEntries = emptyList()
        hasLoadedTodayEntries = true
    }

    private fun upsertWeekEntry(entry: ClockEntry): List<ClockEntry> {
        val existing = weekEntries.indexOfFirst { it.recordId == entry.recordId }
        return if (existing >= 0) {
            weekEntries.toMutableList().also { it[existing] = entry }
        } else {
            weekEntries + entry
        }
    }

    companion object {
        fun dayString(date: Date): String {
            val calendar = Calendar.getInstance()
            calendar.time = date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return "%04d-%02d-%02d".format(year, month, day)
        }

        private fun sampleWeekEntries(): List<ClockEntry> {
            return listOf(
                sampleEntry(daysAgo = 1, startHour = 8, startMinute = 58, endHour = 17, endMinute = 22),
                sampleEntry(daysAgo = 2, startHour = 9, startMinute = 4, endHour = 17, endMinute = 36),
                sampleEntry(daysAgo = 3, startHour = 8, startMinute = 51, endHour = 17, endMinute = 8)
            )
        }

        private fun sampleEntry(
            daysAgo: Int,
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int
        ): ClockEntry {
            val start = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val end = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            return ClockEntry(clockIn = start, clockOut = end, recordId = UUID.randomUUID().toString())
        }
    }
}
