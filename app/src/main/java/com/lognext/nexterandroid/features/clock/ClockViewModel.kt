package com.lognext.nexterandroid.features.clock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.AppConfig
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ClockViewModel(
    private val service: ClockService
) : ViewModel() {
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

    private var hasLoaded = false

    fun loadIfNeeded() {
        if (hasLoaded || AppConfig.UseFakeLogin) return
        hasLoaded = true
        loadFromApi()
    }

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
        if (!AppConfig.UseFakeLogin) {
            clockInWithApi()
            return
        }
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
        if (!AppConfig.UseFakeLogin) {
            clockOutWithApi()
            return
        }
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

    private fun loadFromApi() {
        viewModelScope.launch {
            isLoadingStatus = true
            runCatching {
                val status = service.status()
                val history = service.history(dateFrom = weekStartString(), dateTo = dayString(currentTime))
                status to history.toClockEntries()
            }.onSuccess { (status, entries) ->
                val lastEntry = status.lastEntry?.toClockEntry()
                isCheckedIn = status.isClockedIn
                clockInTime = lastEntry?.clockIn
                clockOutTime = lastEntry?.clockOut
                weekEntries = entries
                todayEntries = entries.filter { dayString(it.clockIn ?: it.clockOut ?: Date(0)) == currentClockDay }
                hasLoadedTodayEntries = true
            }
            isLoadingStatus = false
        }
    }

    private fun clockInWithApi() {
        submittingClockIn = true
        isSubmittingAction = true
        viewModelScope.launch {
            runCatching { service.clockIn() }
                .onSuccess { entry ->
                    clockInTime = entry.clockIn ?: currentTime
                    clockOutTime = null
                    isCheckedIn = true
                    todayEntries = todayEntries + entry
                    weekEntries = upsertWeekEntry(entry)
                }
            isSubmittingAction = false
            submittingClockIn = false
        }
    }

    private fun clockOutWithApi() {
        submittingClockIn = false
        isSubmittingAction = true
        viewModelScope.launch {
            runCatching { service.clockOut() }
                .onSuccess { entry ->
                    val effectiveEntry = entry.takeIf { it.clockIn != null || it.clockOut != null }
                        ?: ClockEntry(clockIn = clockInTime, clockOut = currentTime)
                    clockOutTime = effectiveEntry.clockOut ?: currentTime
                    isCheckedIn = false
                    todayEntries = todayEntries.map { if (it.recordId == effectiveEntry.recordId) effectiveEntry else it }
                        .ifEmpty { listOf(effectiveEntry) }
                    weekEntries = upsertWeekEntry(effectiveEntry)
                }
            isSubmittingAction = false
        }
    }

    private fun weekStartString(): String {
        val calendar = Calendar.getInstance().apply {
            time = currentTime
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return dayString(calendar.time)
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
