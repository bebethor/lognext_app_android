package com.lognext.nexterandroid.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.AppConfig
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
        if (AppConfig.UseFakeLogin) {
            hasLoaded = true
            mutableUiState.value = mockHomeState()
            return
        }

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

    private fun mockHomeState(): HomeUiState {
        return HomeUiState(
            isLoading = false,
            firstName = "Jose",
            vacationDaysRemaining = 18.5,
            meetingsTodayCount = 3,
            pendingTasksCount = 4,
            urgentTasksCount = 1,
            todayMeetings = listOf(
                HomeCalendarEvent(
                    id = "mock-meeting-1",
                    subject = "Daily equipo Nexter",
                    start = "2099-01-01T09:30:00Z",
                    end = "2099-01-01T10:00:00Z",
                    location = null,
                    isOnlineMeeting = true,
                    joinUrl = null,
                    attendeesCount = 6,
                    organizer = "Lognext"
                ),
                HomeCalendarEvent(
                    id = "mock-meeting-2",
                    subject = "Revision de diseño Android",
                    start = "2099-01-01T11:00:00Z",
                    end = "2099-01-01T12:00:00Z",
                    location = "Sala Norte",
                    isOnlineMeeting = false,
                    joinUrl = null,
                    attendeesCount = 4,
                    organizer = "Producto"
                ),
                HomeCalendarEvent(
                    id = "mock-meeting-3",
                    subject = "Planificacion sprint",
                    start = "2099-01-01T16:00:00Z",
                    end = "2099-01-01T17:00:00Z",
                    location = null,
                    isOnlineMeeting = true,
                    joinUrl = null,
                    attendeesCount = 9,
                    organizer = "Equipo"
                )
            ).sortedByStartDate(),
            tasks = listOf(
                HomeTask(
                    id = "mock-task-1",
                    title = "Pulir pantalla Home en Android",
                    description = null,
                    importance = "high",
                    dueDate = "2099-01-01T18:00:00Z",
                    isCompleted = false
                ),
                HomeTask(
                    id = "mock-task-2",
                    title = "Revisar navegacion inferior",
                    description = null,
                    importance = "normal",
                    dueDate = "2099-01-02T10:00:00Z",
                    isCompleted = false
                ),
                HomeTask(
                    id = "mock-task-3",
                    title = "Preparar People",
                    description = null,
                    importance = "low",
                    dueDate = null,
                    isCompleted = false
                )
            ).sortedByDueDate()
        )
    }
}
