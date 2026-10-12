package com.lognext.nexterandroid.features.home

import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService

class HomeService(
    apiClient: APIClient
) : RestService(apiClient) {
    suspend fun getSummary(): HomeSummary {
        val homeSummary = runCatching {
            get("/api/v1/home/summary", HomeSummary::class.java)
        }.getOrNull()
        val me = get("/api/v1/staff/me", StaffMeResponse::class.java)
        val vacationDaysRemaining = homeSummary?.vacationDaysRemaining ?: runCatching {
            get("/api/v1/vacations/balance", VacationBalanceResponse::class.java).totalRemaining
        }.getOrNull()

        val fallbackFirstName = me.firstName.orEmpty().ifBlank { me.fullName.orEmpty().substringBefore(" ").ifBlank { "Lognext" } }
        val fallbackDisplayName = me.fullName.orEmpty().ifBlank { me.firstName.orEmpty().ifBlank { "Lognext" } }

        return homeSummary?.copy(
            firstName = homeSummary.firstName.orEmpty().ifBlank { fallbackFirstName },
            displayName = homeSummary.displayName.orEmpty().ifBlank { fallbackDisplayName },
            positionTitle = me.jobTitle.ifBlank { me.positionTitle },
            vacationDaysRemaining = vacationDaysRemaining
        ) ?: HomeSummary(
            firstName = fallbackFirstName,
            displayName = fallbackDisplayName,
            positionTitle = me.jobTitle.ifBlank { me.positionTitle },
            vacationDaysRemaining = vacationDaysRemaining,
            meetingsTodayCount = null,
            pendingTasksCount = null,
            urgentTasksCount = null
        )
    }

    suspend fun getTodayEvents(): HomeCalendarTodayResponse {
        return get("/api/v1/calendar/today", HomeCalendarTodayResponse::class.java)
    }

    suspend fun listTasks(): HomeTaskListResponse = get("/api/v1/tasks/me", HomeTaskListResponse::class.java)

    suspend fun getEvents(scope: AgendaScope): HomeCalendarTodayResponse {
        return get(scope.endpoint, HomeCalendarTodayResponse::class.java)
    }

    suspend fun createTask(request: HomeTaskCreateRequest): HomeTask? {
        return post("/api/v1/tasks", request, HomeTask::class.java, 201..201)
    }

    suspend fun updateTask(taskId: String, request: HomeTaskUpdateRequest) {
        patch("/api/v1/tasks/$taskId", request, 204..204)
    }

    suspend fun getTask(taskId: String): HomeTask = get("/api/v1/tasks/$taskId", HomeTask::class.java)

    suspend fun editTask(taskId: String, title: String, description: String, importance: String, dueDate: String?) {
        val body = com.google.gson.JsonObject().apply {
            addProperty("title", title)
            addProperty("description", description)
            addProperty("importance", importance)
            if (dueDate == null) add("due_date", com.google.gson.JsonNull.INSTANCE) else addProperty("due_date", dueDate)
        }
        patch("/api/v1/tasks/$taskId", body, 204..204)
    }

    suspend fun createMeeting(request: CreateMeetingRequest): HomeCalendarEvent =
        post("/api/v1/calendar/events", request, HomeCalendarEvent::class.java, 201..201)

    suspend fun deleteTask(taskId: String) {
        delete("/api/v1/tasks/$taskId", 204..204)
    }

    private data class StaffMeResponse(
        @com.google.gson.annotations.SerializedName("full_name") val fullName: String = "",
        @com.google.gson.annotations.SerializedName("first_name") val firstName: String = "",
        @com.google.gson.annotations.SerializedName("job_title") val jobTitle: String = "",
        @com.google.gson.annotations.SerializedName("position_title") val positionTitle: String = ""
    )

    private data class VacationBalanceResponse(
        @com.google.gson.annotations.SerializedName("total_remaining") val totalRemaining: Double = 0.0
    )
}

data class HomeTaskUpdateRequest(
    @com.google.gson.annotations.SerializedName("is_completed") val isCompleted: Boolean? = null,
    val priority: Int? = null,
    val description: String? = null
)
