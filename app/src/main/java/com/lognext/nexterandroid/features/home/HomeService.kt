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

        return homeSummary?.copy(
            firstName = homeSummary.firstName.ifBlank { me.firstName.ifBlank { me.fullName.substringBefore(" ").ifBlank { "Lognext" } } },
            positionTitle = me.jobTitle.ifBlank { me.positionTitle },
            vacationDaysRemaining = vacationDaysRemaining
        ) ?: HomeSummary(
            firstName = me.firstName.ifBlank { me.fullName.substringBefore(" ").ifBlank { "Lognext" } },
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
    @com.google.gson.annotations.SerializedName("percent_complete") val percentComplete: Int? = null,
    val priority: Int? = null,
    val description: String? = null
)
