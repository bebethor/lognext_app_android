package com.lognext.nexterandroid.features.home

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.lognext.nexterandroid.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.util.concurrent.TimeUnit

enum class AgendaScope(@StringRes val titleRes: Int, val endpoint: String, @StringRes val emptyTitleRes: Int) {
    Today(R.string.agenda_scope_today, "/api/v1/calendar/today", R.string.agenda_empty_today),
    Week(R.string.agenda_scope_week, "/api/v1/calendar/week", R.string.agenda_empty_week),
    Upcoming(R.string.agenda_scope_upcoming, "/api/v1/calendar/upcoming", R.string.agenda_empty_upcoming)
}

data class AgendaEventGroup(
    val dateKey: String,
    val title: String,
    val events: List<HomeCalendarEvent>
)

data class HomeSummary(
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("full_name") val displayName: String = "",
    @SerializedName("position_title") val positionTitle: String = "",
    @SerializedName("vacation_days_remaining") val vacationDaysRemaining: Double? = null,
    @SerializedName("meetings_today_count") val meetingsTodayCount: Int? = null,
    @SerializedName("pending_tasks_count") val pendingTasksCount: Int? = null,
    @SerializedName("urgent_tasks_count") val urgentTasksCount: Int? = null
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
    val organizer: String?,
    val attendees: List<MeetingAttendee>? = null,
    @SerializedName("room_reservation") val roomReservation: RoomReservation? = null,
    @SerializedName("can_edit") val canEdit: Boolean = false,
    @SerializedName("event_type") val eventType: String? = null,
    val etag: String? = null
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
    val id: String?,
    val title: String?,
    @SerializedName("plan_id") val planId: String?,
    val description: String?,
    val importance: String?,
    val priority: Int = 5,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("is_completed") val isCompleted: Boolean = false,
    @SerializedName("percent_complete") val percentComplete: Int = 0,
    @SerializedName("created_date") val createdDate: String? = null
) {
    val stableId: String
        get() = id.orEmpty().ifBlank { "${displayTitle}-${dueDate.orEmpty()}" }

    val displayTitle: String
        get() = title.orEmpty().ifBlank { "Sin título" }

    val priorityKey: String
        get() {
            val normalizedImportance = importance.orEmpty().lowercase(Locale.ROOT)
            return when {
                normalizedImportance in setOf("high", "normal", "low") -> normalizedImportance
                priority <= 3 -> "high"
                priority >= 8 -> "low"
                else -> "normal"
            }
        }

    val isDone: Boolean
        get() = isCompleted || percentComplete >= 100

    val creationDate: Date? get() = createdDate?.let(::parseIsoDate)

    val sortDueDate: Date?
        get() = dueDate?.takeIf { it.isNotBlank() }?.let(::parseIsoDate)

    val priorityLabel: String
        get() = when {
            priorityKey == "high" -> "Urgente"
            priorityKey == "low" -> "Baja"
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
    val importance: String,
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
            .thenBy { it.displayTitle.lowercase(Locale.getDefault()) }
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
    val normalizedValue = value.replace(Regex("(\\.\\d{3})\\d+"), "$1").normalizedIsoTimeZone()
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


data class MeetingAttendee(val name: String = "", val email: String = "")

fun List<HomeTask>.sortedByCreationDate(): List<HomeTask> =
    sortedWith(compareByDescending<HomeTask> { it.creationDate?.time ?: Long.MIN_VALUE }.thenBy { it.stableId })

data class MeetingRoom(val email: String, val name: String)
data class RoomReservation(val email: String, val name: String, val status: String)
class RoomBookingUnavailableException : Exception()

internal fun RoomReservation.titleResource(): Int = when (status) {
    "accepted" -> com.lognext.nexterandroid.R.string.meeting_room_accepted
    "declined" -> com.lognext.nexterandroid.R.string.meeting_room_declined
    else -> com.lognext.nexterandroid.R.string.meeting_room_pending
}
internal fun RoomReservation.messageResource(): Int = when (status) {
    "accepted" -> com.lognext.nexterandroid.R.string.meeting_room_accepted_message
    "declined" -> com.lognext.nexterandroid.R.string.meeting_room_declined_message
    else -> com.lognext.nexterandroid.R.string.meeting_room_pending_message
}

data class CreateMeetingRequest(
    val subject: String, val start: String, val end: String,
    val description: String, val location: String,
    @SerializedName("is_online_meeting") val isOnlineMeeting: Boolean,
    val attendees: List<MeetingAttendee>,
    @SerializedName("transaction_id") val transactionId: String,
    val room: MeetingRoom? = null
)

internal fun meetingEditChanges(
    original: HomeCalendarEvent, title: String, start: Date, end: Date, location: String,
    attendees: List<MeetingAttendee>, room: MeetingRoom?, roomChanged: Boolean
): com.google.gson.JsonObject = com.google.gson.JsonObject().apply {
    fun utc(date: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(date)
    if (title.trim() != original.subject) addProperty("subject", title.trim())
    if (start != original.startDate) addProperty("start", utc(start))
    if (end != original.endDate) addProperty("end", utc(end))
    if (location != original.location.orEmpty()) addProperty("location", location)
    if (attendees.map { it.email.lowercase(Locale.ROOT) } != original.attendees.orEmpty().map { it.email.lowercase(Locale.ROOT) }) {
        add("attendees", com.google.gson.Gson().toJsonTree(attendees))
    }
    if (roomChanged) add("room", if (room == null) com.google.gson.JsonNull.INSTANCE else com.google.gson.Gson().toJsonTree(room))
}
