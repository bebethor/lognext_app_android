package com.lognext.nexterandroid.features.home

import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService

class HomeService(
    apiClient: APIClient
) : RestService(apiClient) {
    suspend fun getSummary(): HomeSummary {
        val me = get("/api/v1/staff/me", StaffMeResponse::class.java)
        val tasks = listTasks().tasks
        return HomeSummary(
            firstName = me.firstName.ifBlank { me.fullName.substringBefore(" ").ifBlank { "Lognext" } },
            vacationDaysRemaining = null,
            meetingsTodayCount = null,
            pendingTasksCount = tasks.count { !it.isDone },
            urgentTasksCount = tasks.count { !it.isDone && it.priorityLabel == "Urgente" }
        )
    }

    suspend fun getTodayEvents(): HomeCalendarTodayResponse {
        return HomeCalendarTodayResponse(emptyList())
    }

    suspend fun listTasks(): HomeTaskListResponse = get("/api/v1/tasks/me", HomeTaskListResponse::class.java)

    suspend fun getEvents(@Suppress("UNUSED_PARAMETER") scope: AgendaScope): HomeCalendarTodayResponse {
        return HomeCalendarTodayResponse(emptyList())
    }

    suspend fun createTask(request: HomeTaskCreateRequest): HomeTask {
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
        @com.google.gson.annotations.SerializedName("first_name") val firstName: String = ""
    )
}

data class HomeTaskUpdateRequest(
    @com.google.gson.annotations.SerializedName("percent_complete") val percentComplete: Int? = null,
    val priority: Int? = null,
    val description: String? = null
)
