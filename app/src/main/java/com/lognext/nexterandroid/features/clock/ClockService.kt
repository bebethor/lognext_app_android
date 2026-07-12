package com.lognext.nexterandroid.features.clock

import com.google.gson.annotations.SerializedName
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClockService(
    apiClient: APIClient
) : RestService(apiClient) {
    suspend fun status(): ClockStatusResponse = get("/api/v1/clock/status", ClockStatusResponse::class.java)

    suspend fun history(dateFrom: String? = null, dateTo: String? = null): ClockHistoryResponse {
        return get(
            path = "/api/v1/clock/history",
            type = ClockHistoryResponse::class.java,
            query = mapOf("date_from" to dateFrom, "date_to" to dateTo)
        )
    }

    suspend fun clockIn(notes: String? = null): ClockEntry = post(
        path = "/api/v1/clock/in",
        body = ClockActionRequest(notes),
        type = ClockEntryResponse::class.java,
        acceptedStatusCodes = 201..201
    ).toClockEntry()

    suspend fun clockOut(notes: String? = null): ClockEntry = post(
        path = "/api/v1/clock/out",
        body = ClockActionRequest(notes),
        type = ClockEntryResponse::class.java
    ).toClockEntry()
}

data class ClockStatusResponse(
    @SerializedName("is_clocked_in") val isClockedIn: Boolean = false,
    @SerializedName("last_entry") val lastEntry: ClockEntryResponse? = null
)

data class ClockHistoryResponse(
    val entries: List<ClockEntryResponse> = emptyList(),
    @SerializedName("date_from") val dateFrom: String? = null,
    @SerializedName("date_to") val dateTo: String? = null
) {
    fun toClockEntries(): List<ClockEntry> = entries.map { it.toClockEntry() }
}

data class ClockEntryResponse(
    @SerializedName("clock_in") val clockIn: String? = null,
    @SerializedName("clock_out") val clockOut: String? = null,
    @SerializedName("record_id") val recordId: String = ""
) {
    fun toClockEntry(): ClockEntry {
        return ClockEntry(
            clockIn = clockIn?.takeIf { it.isNotBlank() }?.let(::parseApiDate),
            clockOut = clockOut?.takeIf { it.isNotBlank() }?.let(::parseApiDate),
            recordId = recordId
        )
    }
}

private data class ClockActionRequest(val notes: String?)

private fun parseApiDate(value: String): Date? {
    val normalized = if (value.endsWith("Z")) value else value.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
    return dateParsers.firstNotNullOfOrNull { parser ->
        runCatching { parser.parse(normalized) }.getOrNull()
    }
}

private val dateParsers: List<SimpleDateFormat>
    get() = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    ).onEach { it.timeZone = TimeZone.getTimeZone("UTC") }
