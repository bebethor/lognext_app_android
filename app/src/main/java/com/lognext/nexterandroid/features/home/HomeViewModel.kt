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
    val agendaScope: AgendaScope = AgendaScope.Today,
    val agendaEvents: List<HomeCalendarEvent> = emptyList(),
    val isLoadingAgenda: Boolean = false,
    val hasLoadedAgenda: Boolean = false,
    val agendaErrorMessage: String? = null,
    val tasks: List<HomeTask> = emptyList(),
    val completedTaskIds: Set<String> = emptySet(),
    val isCreatingTask: Boolean = false,
    val createTaskErrorMessage: String? = null,
    val errorMessage: String? = null
) {
    val formattedVacationDays: String
        get() {
            val days = vacationDaysRemaining ?: return "0"
            return if (days % 1.0 == 0.0) days.toInt().toString() else String.format("%.1f", days)
        }

    val displayedPendingTasksCount: Int
        get() = if (tasks.isNotEmpty()) tasks.count { it.id !in completedTaskIds } else pendingTasksCount ?: 0

    val visibleMeetings: List<HomeCalendarEvent>
        get() {
            val now = Date()
            return todayMeetings.filter { (it.endDate ?: Date(Long.MAX_VALUE)).after(now) }.take(3)
        }

    val visibleAgendaEvents: List<HomeCalendarEvent>
        get() {
            val now = Date()
            return agendaEvents.filter { (it.endDate ?: Date(Long.MAX_VALUE)).after(now) }
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
                    tasks = tasks.tasks.sortedByDueDate(),
                    completedTaskIds = tasks.tasks.filter { it.isCompleted }.map { it.id }.toSet()
                )
            }.onFailure { error ->
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo cargar Home."
                )
            }
        }
    }

    fun loadAgenda(scope: AgendaScope, force: Boolean = false) {
        if (AppConfig.UseFakeLogin) {
            mutableUiState.value = mutableUiState.value.copy(
                agendaScope = scope,
                agendaEvents = mockAgendaEvents(scope),
                isLoadingAgenda = false,
                hasLoadedAgenda = true,
                agendaErrorMessage = null
            )
            return
        }

        if (!force && mutableUiState.value.hasLoadedAgenda && mutableUiState.value.agendaScope == scope) return

        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                agendaScope = scope,
                isLoadingAgenda = true,
                agendaErrorMessage = null
            )
            runCatching {
                service.getEvents(scope).events.sortedByStartDate()
            }.onSuccess { events ->
                mutableUiState.value = mutableUiState.value.copy(
                    agendaEvents = events,
                    isLoadingAgenda = false,
                    hasLoadedAgenda = true
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    agendaEvents = emptyList(),
                    isLoadingAgenda = false,
                    hasLoadedAgenda = true,
                    agendaErrorMessage = "Inténtalo de nuevo más tarde."
                )
            }
        }
    }

    fun toggleCompleted(task: HomeTask) {
        val completed = mutableUiState.value.completedTaskIds.toMutableSet()
        if (!completed.add(task.id)) completed.remove(task.id)
        mutableUiState.value = mutableUiState.value.copy(completedTaskIds = completed)
    }

    fun createTask(title: String, description: String, priority: String, dueDate: String?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || mutableUiState.value.isCreatingTask) return

        if (AppConfig.UseFakeLogin) {
            val created = HomeTask(
                id = "mock-task-${System.currentTimeMillis()}",
                title = cleanTitle,
                description = description.trim().ifBlank { null },
                importance = priority,
                dueDate = dueDate?.takeIf { it.isNotBlank() },
                isCompleted = false
            )
            mutableUiState.value = mutableUiState.value.copy(
                tasks = (mutableUiState.value.tasks + created).sortedByDueDate(),
                createTaskErrorMessage = null
            )
            return
        }

        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isCreatingTask = true, createTaskErrorMessage = null)
            val request = HomeTaskCreateRequest(
                title = cleanTitle,
                description = description.trim().ifBlank { null },
                importance = priority,
                dueDate = dueDate?.takeIf { it.isNotBlank() }
            )
            runCatching {
                service.createTask(request)
            }.onSuccess { task ->
                mutableUiState.value = mutableUiState.value.copy(
                    isCreatingTask = false,
                    tasks = (mutableUiState.value.tasks + task).sortedByDueDate()
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    isCreatingTask = false,
                    createTaskErrorMessage = "No se pudo crear la tarea. Revisa los datos e inténtalo de nuevo."
                )
            }
        }
    }

    fun deleteTask(task: HomeTask) {
        if (AppConfig.UseFakeLogin) {
            mutableUiState.value = mutableUiState.value.copy(
                tasks = mutableUiState.value.tasks.filterNot { it.id == task.id },
                completedTaskIds = mutableUiState.value.completedTaskIds - task.id
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                service.deleteTask(task.id)
            }.onSuccess {
                mutableUiState.value = mutableUiState.value.copy(
                    tasks = mutableUiState.value.tasks.filterNot { it.id == task.id },
                    completedTaskIds = mutableUiState.value.completedTaskIds - task.id
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = "No se pudo borrar la tarea. Inténtalo de nuevo."
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
            ).sortedByDueDate(),
            completedTaskIds = emptySet()
        )
    }

    private fun mockAgendaEvents(scope: AgendaScope): List<HomeCalendarEvent> {
        val baseEvents = mockHomeState().todayMeetings
        if (scope == AgendaScope.Today) return baseEvents
        val extra = listOf(
            HomeCalendarEvent(
                id = "mock-agenda-4",
                subject = "Seguimiento cliente Lognext",
                start = "2099-01-02T10:30:00Z",
                end = "2099-01-02T11:15:00Z",
                location = "Sala Sur",
                isOnlineMeeting = false,
                joinUrl = null,
                attendeesCount = 5,
                organizer = "Comercial"
            ),
            HomeCalendarEvent(
                id = "mock-agenda-5",
                subject = "Demo interna StaffHub",
                start = "2099-01-04T12:00:00Z",
                end = "2099-01-04T13:00:00Z",
                location = null,
                isOnlineMeeting = true,
                joinUrl = null,
                attendeesCount = 12,
                organizer = "Producto"
            )
        )
        return (baseEvents + extra).sortedByStartDate()
    }
}
