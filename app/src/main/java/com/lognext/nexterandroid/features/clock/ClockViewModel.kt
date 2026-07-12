package com.lognext.nexterandroid.features.clock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.core.network.APIError
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
    var weekEntries by mutableStateOf(if (AppConfig.UseFakeLogin) sampleWeekEntries() else emptyList())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoadingStatus by mutableStateOf(false)
        private set
    var hasLoadedTodayEntries by mutableStateOf(AppConfig.UseFakeLogin)
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
        val effectiveClockOut = entry.clockOut ?: if (isCheckedIn && dayString(clockIn) == currentClockDay) currentTime else null
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
        weekEntries = emptyList()
        hasLoadedTodayEntries = false
        hasLoaded = false
        if (!AppConfig.UseFakeLogin) loadFromApi()
    }

    private fun loadFromApi() {
        viewModelScope.launch {
            isLoadingStatus = true
            hasLoadedTodayEntries = false
            val weekRange = currentWeekRange()
            val statusResult = runCatching { service.status() }
            val todayResult = runCatching {
                service.history(dateFrom = currentClockDay, dateTo = currentClockDay).toClockEntries()
            }
            val weekResult = runCatching {
                service.history(dateFrom = weekRange.first, dateTo = weekRange.second).toClockEntries()
            }

            statusResult.onSuccess(::applyStatus)
            todayResult.onSuccess { today ->
                todayEntries = today
                reconcileOpenEntryFromTodayHistory()
            }
            weekResult.onSuccess { week ->
                weekEntries = mergeEntries(week, todayEntries)
            }

            hasLoadedTodayEntries = true
            errorMessage = listOf(statusResult, todayResult, weekResult)
                .firstOrNull { it.isFailure }
                ?.exceptionOrNull()
                ?.let { "No se pudieron cargar todos los datos de jornada." }
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
                    errorMessage = null
                    loadTodayHistory()
                }
                .onFailure { error ->
                    errorMessage = if (error is APIError.Http && error.statusCode == 409) {
                        loadFromApi()
                        "Ya tienes una entrada abierta. Primero debes fichar salida."
                    } else {
                        "No se pudo registrar la entrada. Inténtalo de nuevo en unos minutos."
                    }
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
                    errorMessage = null
                    loadTodayHistory()
                }
                .onFailure { error ->
                    errorMessage = if (error is APIError.Http && error.statusCode == 409) {
                        loadFromApi()
                        "No tienes ninguna entrada abierta. Primero debes fichar entrada."
                    } else {
                        "No se pudo registrar la salida. Inténtalo de nuevo en unos minutos."
                    }
                }
            isSubmittingAction = false
        }
    }

    private fun currentWeekRange(): Pair<String, String> {
        val calendar = Calendar.getInstance().apply {
            time = currentTime
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val start = dayString(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        return start to dayString(calendar.time)
    }

    private fun loadTodayHistory() {
        viewModelScope.launch {
            runCatching {
                service.history(dateFrom = currentClockDay, dateTo = currentClockDay).toClockEntries()
            }.onSuccess { entries ->
                todayEntries = entries
                reconcileOpenEntryFromTodayHistory()
                weekEntries = mergeEntries(weekEntries, entries)
                hasLoadedTodayEntries = true
            }
        }
    }

    private fun applyStatus(status: ClockStatusResponse) {
        val lastEntry = status.lastEntry?.toClockEntry()
        val clockInBelongsToToday = lastEntry?.clockIn?.let { dayString(it) == currentClockDay } == true
        val clockOutBelongsToToday = lastEntry?.clockOut?.let { dayString(it) == currentClockDay } == true
        isCheckedIn = status.isClockedIn && clockInBelongsToToday
        clockInTime = if (clockInBelongsToToday) lastEntry?.clockIn else null
        clockOutTime = if (isCheckedIn) null else if (clockOutBelongsToToday) lastEntry?.clockOut else null
    }

    private fun reconcileOpenEntryFromTodayHistory() {
        val openEntry = todayEntries.lastOrNull {
            it.clockOut == null && it.clockIn?.let { date -> dayString(date) == currentClockDay } == true
        } ?: return
        isCheckedIn = true
        clockInTime = openEntry.clockIn
        clockOutTime = null
    }

    private fun upsertWeekEntry(entry: ClockEntry): List<ClockEntry> {
        val existing = weekEntries.indexOfFirst { it.recordId == entry.recordId }
        return if (existing >= 0) {
            weekEntries.toMutableList().also { it[existing] = entry }
        } else {
            weekEntries + entry
        }
    }

    private fun mergeEntries(primary: List<ClockEntry>, secondary: List<ClockEntry>): List<ClockEntry> {
        return secondary.fold(primary) { acc, entry ->
            val existing = acc.indexOfFirst {
                it.recordId.isNotBlank() && it.recordId == entry.recordId
            }
            if (existing >= 0) {
                acc.toMutableList().also { it[existing] = entry }
            } else {
                acc + entry
            }
        }.sortedBy { it.clockIn ?: it.clockOut ?: Date(0) }
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
