package com.lognext.nexterandroid.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class HomeUiState(
    val isLoading: Boolean = false,
    val firstName: String = "",
    val vacationDaysRemaining: Double? = null,
    val meetingsTodayCount: Int? = null,
    val pendingTasksCount: Int? = null,
    val urgentTasksCount: Int? = null,
    val todayMeetings: List<HomeCalendarEvent> = emptyList(),
    val tasks: List<HomeTask> = emptyList(),
    val errorMessage: String? = null
) {
    val formattedVacationDays: String
        get() {
            val days = vacationDaysRemaining ?: return "0"
            return if (days % 1.0 == 0.0) days.toInt().toString() else String.format("%.1f", days)
        }

    val displayedPendingTasksCount: Int
        get() = if (tasks.isNotEmpty()) tasks.count { !it.isCompleted } else pendingTasksCount ?: 0

    val visibleMeetings: List<HomeCalendarEvent>
        get() {
            val now = Date()
            return todayMeetings.filter { (it.endDate ?: Date(Long.MAX_VALUE)).after(now) }.take(3)
        }
}

class HomeViewModel(
    private val service: HomeService
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private var hasLoaded = false

    fun loadIfNeeded() {
        if (hasLoaded || mutableUiState.value.isLoading && mutableUiState.value.firstName.isNotBlank()) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                val summary = async { service.getSummary() }
                val meetings = async { service.getTodayEvents() }
                val tasks = async { service.listTasks() }

                Triple(summary.await(), meetings.await(), tasks.await())
            }.onSuccess { (summary, meetings, tasks) ->
                hasLoaded = true
                mutableUiState.value = HomeUiState(
                    isLoading = false,
                    firstName = summary.firstName,
                    vacationDaysRemaining = summary.vacationDaysRemaining,
                    meetingsTodayCount = summary.meetingsTodayCount,
                    pendingTasksCount = summary.pendingTasksCount,
                    urgentTasksCount = summary.urgentTasksCount,
                    todayMeetings = meetings.events.sortedByStartDate(),
                    tasks = tasks.tasks.sortedByDueDate()
                )
            }.onFailure { error ->
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo cargar Home."
                )
            }
        }
    }
}
