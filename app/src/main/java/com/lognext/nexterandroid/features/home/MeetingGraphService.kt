package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.Normalizer
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun interface MeetingGraphTransport { suspend fun execute(request: Request): String }
internal class MeetingGraphHttpException(val status: Int) : IOException("Microsoft Graph HTTP $status")

private class GraphTransport : MeetingGraphTransport {
    // Keep Graph credentials separate from the backend interceptor and response cache.
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    override suspend fun execute(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) throw MeetingGraphHttpException(it.code)
                        val body = it.body?.string() ?: throw IOException("Missing Graph response")
                        if (continuation.isActive) continuation.resume(body)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            }
        })
    }
}

internal enum class MeetingAvailability {
    Available, Tentative, Busy, OutOfOffice, Unknown;
    val hasConflict get() = this in listOf(Tentative, Busy, OutOfOffice)
    companion object {
        fun combined(values: List<MeetingAvailability>): MeetingAvailability =
            listOf(OutOfOffice, Busy, Tentative, Unknown).firstOrNull { it in values } ?: Available
    }
}

internal data class GraphScheduleTime(val dateTime: String, val timeZone: String) {
    fun date(): Date? {
        val hasOffset = Regex("(Z|[+-]\\d{2}:\\d{2})$").containsMatchIn(dateTime)
        if (!hasOffset && timeZone.lowercase(Locale.ROOT) !in listOf("utc", "etc/utc")) return null
        var value = if (hasOffset) dateTime else dateTime + "Z"
        val fraction = Regex("\\.(\\d+)").find(value)
        if (fraction != null) value = value.replaceRange(fraction.range, "." + fraction.groupValues[1].take(3).padEnd(3, '0'))
        val pattern = if (fraction != null) "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" else "yyyy-MM-dd'T'HH:mm:ssXXX"
        val formatter = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
        val position = ParsePosition(0)
        return formatter.parse(value, position)?.takeIf { position.index == value.length }
    }
}
internal data class GraphScheduleItem(val status: String?, val start: GraphScheduleTime?, val end: GraphScheduleTime?)
internal data class GraphSchedule(val scheduleId: String?, val availabilityView: String?, val scheduleItems: List<GraphScheduleItem>?, val error: Any?) {
    fun availability(start: Date, end: Date): MeetingAvailability {
        if (error != null || !start.before(end)) return MeetingAvailability.Unknown
        val items = scheduleItems
        if (items != null && !(items.isEmpty() && !availabilityView.isNullOrEmpty())) {
            val states = mutableListOf<MeetingAvailability>()
            for (item in items) {
                val from = item.start?.date() ?: return MeetingAvailability.Unknown
                val to = item.end?.date() ?: return MeetingAvailability.Unknown
                if (!from.before(to)) return MeetingAvailability.Unknown
                // An event touching an interval boundary does not overlap the meeting.
                if (!from.before(end) || !to.after(start)) continue
                states += when (item.status?.lowercase(Locale.ROOT)) {
                    "free", "workingelsewhere" -> MeetingAvailability.Available
                    "tentative" -> MeetingAvailability.Tentative
                    "busy" -> MeetingAvailability.Busy
                    "oof" -> MeetingAvailability.OutOfOffice
                    else -> MeetingAvailability.Unknown
                }
            }
            return MeetingAvailability.combined(states)
        }
        if (availabilityView.isNullOrEmpty()) return MeetingAvailability.Unknown
        return MeetingAvailability.combined(availabilityView.map {
            when (it) { '0' -> MeetingAvailability.Available; '1' -> MeetingAvailability.Tentative
                '2' -> MeetingAvailability.Busy; '3' -> MeetingAvailability.OutOfOffice; else -> MeetingAvailability.Unknown }
        })
    }
}
internal enum class MeetingLocationKind { Room, Office, RoomGroup }
internal data class MeetingLocation(val id: String, val displayName: String, val kind: MeetingLocationKind,
    val building: String? = null, val city: String? = null, val capacity: Int? = null) {
    val locationName: String get() {
        val context = listOfNotNull(building, city).firstOrNull { it.isNotBlank() }
        return if (kind == MeetingLocationKind.Room && context != null && !displayName.contains(context, true)) "$displayName · $context" else displayName
    }
    fun matches(query: String): Boolean {
        fun fold(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        val text = fold(listOfNotNull(displayName, building, city).joinToString(" "))
        return fold(query).split(Regex("\\s+")).filter { it.isNotEmpty() }.all { text.contains(it) }
    }
}
internal data class MeetingLocationCatalog(val locations: List<MeetingLocation>, val isPartial: Boolean)
private data class GraphPlaces(val value: List<GraphPlaceRecord>?, @SerializedName("@odata.nextLink") val nextLink: String?)
private data class GraphPlaceRecord(val id: String?, val displayName: String?, val building: String?, val capacity: Int?, val address: Address?) {
    data class Address(val city: String?)
}
private data class GraphSchedules(val value: List<GraphSchedule>?)

internal class MeetingGraphService(private val transport: MeetingGraphTransport = GraphTransport()) {
    private val gson = Gson()
    suspend fun availability(token: String, emails: List<String>, start: Date, end: Date): Map<String, MeetingAvailability> {
        require(start.before(end) && end.time - start.time < 62L * 24 * 60 * 60 * 1000)
        val normalized = emails.map { it.trim().lowercase(Locale.ROOT) }.distinct().sorted()
        val result = normalized.associateWith { MeetingAvailability.Unknown }.toMutableMap()
        fun time(date: Date) = GraphScheduleTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date), "UTC")
        for (batch in normalized.chunked(20)) {
            currentCoroutineContext().ensureActive()
            val payload = gson.toJson(mapOf("schedules" to batch, "startTime" to time(start), "endTime" to time(end), "availabilityViewInterval" to 5))
            val request = request("https://graph.microsoft.com/v1.0/me/calendar/getSchedule".toHttpUrl(), token)
                .header("Prefer", "outlook.timezone=\"UTC\"")
                .post(payload.toRequestBody("application/json".toMediaType())).build()
            val schedules = gson.fromJson(transport.execute(request), GraphSchedules::class.java).value ?: throw IOException("Missing schedules")
            currentCoroutineContext().ensureActive()
            for (schedule in schedules) {
                val email = schedule.scheduleId?.trim()?.lowercase(Locale.ROOT) ?: continue
                if (email in batch) result[email] = schedule.availability(start, end)
            }
        }
        return result
    }
    suspend fun locations(token: String): MeetingLocationCatalog {
        val places = mutableListOf<MeetingLocation>()
        var partial = false
        for ((type, kind) in listOf("room" to MeetingLocationKind.Room, "building" to MeetingLocationKind.Office, "roomList" to MeetingLocationKind.RoomGroup)) {
            val select = if (type == "room") "id,displayName,building,capacity,address" else "id,displayName"
            var url: HttpUrl? = "https://graph.microsoft.com/v1.0/places/microsoft.graph.$type".toHttpUrl().newBuilder()
                .addQueryParameter("\$select", select).addQueryParameter("\$top", "100").build()
            val visited = mutableSetOf<HttpUrl>()
            try {
                while (url != null) {
                    currentCoroutineContext().ensureActive()
                    val current = url
                    check(visited.size < 100 && visited.add(current)) { "Invalid Graph pagination" }
                    val response = gson.fromJson(transport.execute(request(current, token).build()), GraphPlaces::class.java)
                    currentCoroutineContext().ensureActive()
                    val records = response.value ?: throw IOException("Missing places")
                    places += records.filter { !it.id.isNullOrBlank() && !it.displayName.isNullOrBlank() }.map {
                        MeetingLocation("$type|${it.id}", it.displayName!!, kind, it.building, it.address?.city, it.capacity)
                    }
                    url = response.nextLink?.let { link ->
                        val next = link.toHttpUrlOrNull()
                        check(next != null && next.scheme == "https" && next.host == "graph.microsoft.com" && next.port == 443 &&
                            next.username.isEmpty() && next.password.isEmpty() && next.encodedPath == current.encodedPath) { "Invalid Graph pagination" }
                        next
                    }
                }
            } catch (failure: MeetingGraphHttpException) {
                if (type == "room" || failure.status !in listOf(400, 403, 404)) throw failure
                partial = true
            }
        }
        val unique = places.distinctBy { if (it.kind == MeetingLocationKind.Room) it.id else "office|" + it.displayName.lowercase(Locale.ROOT) }
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }
        return MeetingLocationCatalog(unique, partial)
    }
    private fun request(url: HttpUrl, token: String): Request.Builder = Request.Builder().url(url)
        .header("Authorization", "Bearer $token").header("Accept", "application/json")
}
