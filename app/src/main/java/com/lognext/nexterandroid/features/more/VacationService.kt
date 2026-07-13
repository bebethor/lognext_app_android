package com.lognext.nexterandroid.features.more

import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.APIError
import com.lognext.nexterandroid.core.network.RestService
import java.util.Calendar

class VacationService(
    apiClient: APIClient
) : RestService(apiClient) {
    suspend fun balance(): MoreVacationBalance {
        return try {
            get("/api/v1/vacations/balance", MoreVacationBalance::class.java)
        } catch (error: APIError.Http) {
            if (error.statusCode == 404 && error.serverMessage.contains("No Holiday absence plan configured", ignoreCase = true)) {
                MoreVacationBalance(canRequest = false)
            } else {
                throw error
            }
        }
    }

    suspend fun history(dateFrom: String? = null, dateTo: String? = null): VacationHistoryResponse {
        val response = get(
            path = "/api/v1/vacations/history",
            type = VacationHistoryResponse::class.java,
            query = mapOf("date_from" to dateFrom, "date_to" to dateTo)
        )
        if (response.entries.isNotEmpty() || dateFrom == null || dateTo == null) return response
        return get("/api/v1/vacations/history", VacationHistoryResponse::class.java)
    }

    suspend fun currentYearHistory(): VacationHistoryResponse {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return history("$year-01-01", "$year-12-31")
    }

    suspend fun plans(): VacationPlansResponse {
        return get("/api/v1/vacations/plans", VacationPlansResponse::class.java)
    }

    suspend fun types(): VacationTypesResponse {
        return get("/api/v1/vacations/types", VacationTypesResponse::class.java)
    }

    suspend fun create(request: VacationCreateRequest): VacationCreateResponse {
        return post(
            path = "/api/v1/vacations",
            body = request,
            type = VacationCreateResponse::class.java,
            acceptedStatusCodes = 200..201
        )
    }
}
