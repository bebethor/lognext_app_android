package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class HomeService(
    private val apiClient: APIClient,
    private val gson: Gson = Gson()
) {
    suspend fun getSummary(): HomeSummary = get("/api/v1/home/summary", HomeSummary::class.java)

    suspend fun getTodayEvents(): HomeCalendarTodayResponse {
        return get("/api/v1/calendar/today", HomeCalendarTodayResponse::class.java)
    }

    suspend fun listTasks(): HomeTaskListResponse = get("/api/v1/tasks/me", HomeTaskListResponse::class.java)

    suspend fun getEvents(scope: AgendaScope): HomeCalendarTodayResponse {
        return get(scope.endpoint, HomeCalendarTodayResponse::class.java)
    }

    suspend fun createTask(request: HomeTaskCreateRequest): HomeTask {
        return withContext(Dispatchers.IO) {
            val body = gson.toJson(request)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(AppConfig.BaseUrl.trimEnd('/') + "/api/v1/tasks")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            gson.fromJson(apiClient.execute(httpRequest, 201..201), HomeTask::class.java)
        }
    }

    suspend fun deleteTask(taskId: String) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(AppConfig.BaseUrl.trimEnd('/') + "/api/v1/tasks/$taskId")
                .header("Accept", "application/json")
                .delete()
                .build()

            apiClient.execute(request, 204..204)
        }
    }

    private suspend fun <T> get(path: String, type: Class<T>): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(AppConfig.BaseUrl.trimEnd('/') + path)
                .header("Accept", "application/json")
                .get()
                .build()

            gson.fromJson(apiClient.execute(request), type)
        }
    }
}
