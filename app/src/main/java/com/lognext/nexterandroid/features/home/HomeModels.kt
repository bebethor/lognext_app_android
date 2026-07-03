package com.lognext.nexterandroid.features.home

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.util.concurrent.TimeUnit

data class HomeSummary(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("vacation_days_remaining") val vacationDaysRemaining: Double?,
    @SerializedName("meetings_today_count") val meetingsTodayCount: Int?,
    @SerializedName("pending_tasks_count") val pendingTasksCount: Int?,
    @SerializedName("urgent_tasks_count") val urgentTasksCount: Int?
)

data class HomeCalendarTodayResponse(
    val events: List<HomeCalendarEvent>
)

data class HomeCalendarEvent(
    val id: String,
    val subject: String,
    val start: String,
    val end: String,
    val location: String?,
    @SerializedName("is_online_meeting") val isOnlineMeeting: Boolean,
    @SerializedName("join_url") val joinUrl: String?,
    @SerializedName("attendees_count") val attendeesCount: Int?,
    val organizer: String?
) {
    val startDate: Date?
        get() = parseIsoDate(start)

    val endDate: Date?
        get() = parseIsoDate(end)

    val startTimeText: String
        get() = startDate?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "--:--"

    val subtitle: String
        get() {
            val parts = mutableListOf<String>()
            durationText()?.let { parts.add(it) }
            when {
                isOnlineMeeting -> parts.add("Teams")
                !location.isNullOrBlank() -> parts.add(location.trim())
            }
            attendeesCount?.takeIf { it > 0 }?.let { parts.add("$it asistentes") }
            return if (parts.isEmpty()) "Reunion" else parts.joinToString(" · ")
        }

    val tagText: String?
        get() = when {
            isOnlineMeeting -> "Online"
            !location.isNullOrBlank() -> "Presencial"
            else -> null
        }

    private fun durationText(): String? {
        val startDate = startDate ?: return null
        val endDate = endDate ?: return null
        val minutes = TimeUnit.MILLISECONDS.toMinutes((endDate.time - startDate.time).coerceAtLeast(0))
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0L) "${hours}h" else "${hours}h ${remainingMinutes}min"
    }
}

data class HomeTaskListResponse(
    val tasks: List<HomeTask>
)

data class HomeTask(
    val id: String,
    val title: String,
    val description: String?,
    val importance: String,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("is_completed") val isCompleted: Boolean
) {
    val sortDueDate: Date?
        get() = dueDate?.takeIf { it.isNotBlank() }?.let(::parseIsoDate)

    val priorityLabel: String
        get() = when (importance.lowercase(Locale.ROOT)) {
            "high" -> "Urgente"
            "low" -> "Baja"
            else -> "Media"
        }

    val dueDateText: String?
        get() = sortDueDate?.let {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(it).replace(".", "")
        }
}

fun List<HomeCalendarEvent>.sortedByStartDate(): List<HomeCalendarEvent> {
    return sortedWith(
        compareBy<HomeCalendarEvent> { it.startDate ?: Date(Long.MAX_VALUE) }
            .thenBy { it.subject.lowercase(Locale.getDefault()) }
    )
}

fun List<HomeTask>.sortedByDueDate(): List<HomeTask> {
    return sortedWith(
        compareBy<HomeTask> { it.sortDueDate ?: Date(Long.MAX_VALUE) }
            .thenBy { it.title.lowercase(Locale.getDefault()) }
    )
}

private fun parseIsoDate(value: String): Date? {
    return isoParsers.firstNotNullOfOrNull { parser ->
        runCatching { parser.parse(value) }.getOrNull()
    }
}

private val isoParsers: List<SimpleDateFormat>
    get() = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    ).onEach {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }
