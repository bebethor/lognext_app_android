package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class HomeService(
    private val apiClient: APIClient,
    private val gson: Gson = Gson()
) {
    suspend fun getSummary(): HomeSummary = get("/api/v1/home/summary", HomeSummary::class.java)

    suspend fun getTodayEvents(): HomeCalendarTodayResponse {
        return get("/api/v1/calendar/today", HomeCalendarTodayResponse::class.java)
    }

    suspend fun listTasks(): HomeTaskListResponse = get("/api/v1/tasks/me", HomeTaskListResponse::class.java)

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
