package com.lognext.nexterandroid.features.home

import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class HomeService(
    apiClient: APIClient
) : RestService(apiClient) {
    private val editingApiClient = apiClient

    private fun meetingRequest(id: String): okhttp3.Request.Builder = okhttp3.Request.Builder()
        .url(com.lognext.nexterandroid.core.AppConfig.BaseUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegments("api/v1/calendar/events").addPathSegment(id).build())
        .header("Accept", "application/json")

    suspend fun getMeeting(id: String): HomeCalendarEvent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        com.google.gson.Gson().fromJson(editingApiClient.execute(meetingRequest(id)
            .header("Cache-Control", "no-store").get().build()), HomeCalendarEvent::class.java)
    }

    suspend fun updateMeeting(id: String, changes: com.google.gson.JsonObject): HomeCalendarEvent =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = meetingRequest(id).header("Content-Type", "application/json")
                .patch(changes.toString().toRequestBody("application/json".toMediaType())).build()
            com.google.gson.Gson().fromJson(editingApiClient.execute(request, 200..200), HomeCalendarEvent::class.java)
        }

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

    private data class BookingCapabilities(@com.google.gson.annotations.SerializedName("resource_booking") val resourceBooking: Boolean)

    suspend fun createMeeting(request: CreateMeetingRequest): HomeCalendarEvent {
        require(request.isOnlineMeeting || request.location.isNotBlank())
        if (request.room != null) {
            try {
                val capabilities = get("/api/v1/calendar/booking-capabilities", BookingCapabilities::class.java)
                if (!capabilities.resourceBooking) throw RoomBookingUnavailableException()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { throw RoomBookingUnavailableException() }
        }
        return post("/api/v1/calendar/events", request, HomeCalendarEvent::class.java, 201..201)
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
    @com.google.gson.annotations.SerializedName("is_completed") val isCompleted: Boolean? = null,
    val priority: Int? = null,
    val description: String? = null
)
