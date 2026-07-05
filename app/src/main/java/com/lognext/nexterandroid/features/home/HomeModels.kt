package com.lognext.nexterandroid.features.home

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.util.concurrent.TimeUnit

enum class AgendaScope(val title: String, val endpoint: String, val emptyTitle: String) {
    Today("Hoy", "/api/v1/calendar/today", "No tienes reuniones hoy"),
    Week("Semana", "/api/v1/calendar/week", "No tienes reuniones esta semana"),
    Upcoming("Próximas", "/api/v1/calendar/upcoming", "No tienes próximas reuniones")
}

data class AgendaEventGroup(
    val dateKey: String,
    val title: String,
    val events: List<HomeCalendarEvent>
)

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

    val dayKey: String
        get() = startDate?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        } ?: start

    val dayTitle: String
        get() {
            val date = startDate ?: return "Sin fecha"
            val today = Calendar.getInstance()
            val eventDay = Calendar.getInstance().apply { time = date }
            if (today.sameDay(eventDay)) return "Hoy"
            today.add(Calendar.DAY_OF_YEAR, 1)
            if (today.sameDay(eventDay)) return "Mañana"
            return SimpleDateFormat("EEEE d MMM", Locale.getDefault())
                .format(date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

    val subtitle: String
        get() {
            val parts = mutableListOf<String>()
            durationText()?.let { parts.add(it) }
            when {
                isOnlineMeeting -> parts.add("Teams")
                !location.isNullOrBlank() -> parts.add(location.trim())
            }
            attendeesCount?.takeIf { it > 0 }?.let { parts.add("$it asistentes") }
            return if (parts.isEmpty()) "Reunión" else parts.joinToString(" · ")
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
    @SerializedName("plan_id") val planId: String = "",
    val description: String?,
    val importance: String = "",
    val priority: Int = 5,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("is_completed") val isCompleted: Boolean = false,
    @SerializedName("percent_complete") val percentComplete: Int = 0
) {
    val isDone: Boolean
        get() = isCompleted || percentComplete >= 100

    val sortDueDate: Date?
        get() = dueDate?.takeIf { it.isNotBlank() }?.let(::parseIsoDate)

    val priorityLabel: String
        get() = when {
            importance.equals("high", ignoreCase = true) || priority >= 8 -> "Urgente"
            importance.equals("low", ignoreCase = true) || priority <= 3 -> "Baja"
            else -> "Media"
        }

    val dueDateText: String?
        get() = sortDueDate?.let {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(it).replace(".", "")
        }
}

data class HomeTaskCreateRequest(
    val title: String,
    @SerializedName("plan_id") val planId: String,
    val description: String?,
    val priority: Int,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("start_date") val startDate: String? = null
)

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

fun List<HomeCalendarEvent>.groupedByDay(): List<AgendaEventGroup> {
    return groupBy { it.dayKey }
        .map { (key, events) ->
            AgendaEventGroup(
                dateKey = key,
                title = events.firstOrNull()?.dayTitle ?: key,
                events = events.sortedByStartDate()
            )
        }
        .sortedWith(compareBy { group -> group.events.firstOrNull()?.startDate ?: Date(Long.MAX_VALUE) })
}

private fun Calendar.sameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun parseIsoDate(value: String): Date? {
    val normalizedValue = value.normalizedIsoTimeZone()
    return isoParsers.firstNotNullOfOrNull { parser ->
        runCatching { parser.parse(normalizedValue) }.getOrNull()
    }
}

private val isoParsers: List<SimpleDateFormat>
    get() = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    ).onEach {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

private fun String.normalizedIsoTimeZone(): String {
    if (endsWith("Z")) return this
    return replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
}
