package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class MeetingGraphServiceTest {
    private val start = GraphScheduleTime("2026-10-14T10:00:00", "UTC").date()!!
    private val end = GraphScheduleTime("2026-10-14T11:00:00", "UTC").date()!!
    private fun schedule(items: String? = null, view: String? = null, error: String? = null): GraphSchedule = Gson().fromJson(
        """{"scheduleId":"a@example.com","scheduleItems":${items ?: "null"},"availabilityView":${view?.let { "\"$it\"" } ?: "null"},"error":${error ?: "null"}}""", GraphSchedule::class.java)
    private fun item(status: String, from: String, to: String) = """{"status":"$status","start":{"dateTime":"$from","timeZone":"UTC"},"end":{"dateTime":"$to","timeZone":"UTC"}}"""

    @Test fun intervalBoundariesDoNotConflict() {
        val items = "[${item("busy", "2026-10-14T09:00:00", "2026-10-14T10:00:00")},${item("busy", "2026-10-14T11:00:00", "2026-10-14T12:00:00")}]"
        assertEquals(MeetingAvailability.Available, schedule(items).availability(start, end))
        assertEquals(MeetingAvailability.Busy, schedule("[${item("busy", "2026-10-14T10:59:00", "2026-10-14T12:00:00")}]").availability(start, end))
    }
    @Test fun statusSeverityAndUnknownCalendarsMatchIos() {
        for ((raw, state) in listOf("free" to MeetingAvailability.Available, "workingElsewhere" to MeetingAvailability.Available,
            "tentative" to MeetingAvailability.Tentative, "busy" to MeetingAvailability.Busy, "oof" to MeetingAvailability.OutOfOffice, "other" to MeetingAvailability.Unknown)) {
            assertEquals(state, schedule("[${item(raw, "2026-10-14T10:15:00", "2026-10-14T10:45:00")}]").availability(start, end))
        }
        assertEquals(MeetingAvailability.OutOfOffice, schedule(view = "0123").availability(start, end))
        assertEquals(MeetingAvailability.Busy, schedule(items = "[]", view = "0020").availability(start, end))
        assertEquals(MeetingAvailability.Unknown, schedule(view = "000", error = "{\"responseCode\":\"5006\"}").availability(start, end))
        assertEquals(MeetingAvailability.Unknown, schedule().availability(start, end))
        assertEquals(MeetingAvailability.Unknown, schedule(view = "0x").availability(start, end))
    }
    @Test fun datesHandleOffsetsAndSevenFractionDigitsWithoutAssumingUnknownTimezone() {
        assertEquals(start.time + 123, GraphScheduleTime("2026-10-14T10:00:00.1234567", "UTC").date()!!.time)
        assertEquals(start, GraphScheduleTime("2026-10-14T12:00:00+02:00", "unneeded").date())
        assertNull(GraphScheduleTime("2026-10-14T10:00:00", "Europe/Madrid").date())
        assertNull(GraphScheduleTime("invalid", "UTC").date())
        assertNull(GraphScheduleTime("2026-02-31T10:00:00", "UTC").date())
    }
    @Test fun batchesTwentyCalendarsUsesGraphTokenAndUtcAndMissingRowsStayUnknown() = runBlocking {
        val requests = mutableListOf<Request>()
        val service = MeetingGraphService { request ->
            requests += request
            assertEquals("graph.microsoft.com", request.url.host)
            assertEquals("/v1.0/me/calendar/getSchedule", request.url.encodedPath)
            assertEquals("POST", request.method)
            assertEquals("Bearer graph-token", request.header("Authorization"))
            assertEquals("outlook.timezone=\"UTC\"", request.header("Prefer"))
            assertNull(request.header("Cache-Control"))
            val buffer = Buffer(); request.body!!.writeTo(buffer)
            val body = JsonParser.parseString(buffer.readUtf8()).asJsonObject
            assertEquals("UTC", body["startTime"].asJsonObject["timeZone"].asString)
            assertEquals("2026-10-14T10:00:00", body["startTime"].asJsonObject["dateTime"].asString)
            assertTrue(body["schedules"].asJsonArray.size() <= 20)
            """{"value":[{"scheduleId":"P1@EXAMPLE.COM","availabilityView":"2"}]}"""
        }
        val emails = (1..21).map { "p$it@example.com" } + " P1@EXAMPLE.COM "
        val result = service.availability("graph-token", emails, start, end)
        assertEquals(2, requests.size)
        assertEquals(21, result.size)
        assertEquals(MeetingAvailability.Busy, result["p1@example.com"])
        assertEquals(MeetingAvailability.Unknown, result["p2@example.com"])
    }
    @Test fun invalidIntervalsNeverCallGraph() = runBlocking {
        val service = MeetingGraphService { fail("Must not call Graph"); "" }
        for (to in listOf(start, Date(start.time - 1000), Date(start.time + 62L * 24 * 60 * 60 * 1000))) {
            try { service.availability("token", listOf("a@example.com"), start, to); fail("Invalid interval") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun cancellationStopsFurtherRequests() = runBlocking {
        var count = 0
        val service = MeetingGraphService { count++; delay(500); "{\"value\":[]}" }
        try { withTimeout(50) { service.availability("token", (1..30).map { "p$it@example.com" }, start, end) }; fail("Expected cancellation") }
        catch (_: kotlinx.coroutines.TimeoutCancellationException) { }
        assertEquals(1, count)
    }
    @Test fun locationPaginationDeduplicationAndAccentSearch() = runBlocking {
        val requests = mutableListOf<Request>()
        val service = MeetingGraphService { request ->
            requests += request
            assertEquals("Bearer token", request.header("Authorization"))
            when {
                request.url.queryParameter("page") == "2" -> """{"value":[{"id":"r2","displayName":"Sala Sur","address":{"city":"Sevilla"}}]}"""
                request.url.encodedPath.endsWith(".room") -> """{"value":[{"id":"r1","displayName":"Sala Águila","building":"Madrid","capacity":8}],"@odata.nextLink":"https://graph.microsoft.com/v1.0/places/microsoft.graph.room?page=2"}"""
                request.url.encodedPath.endsWith(".building") -> """{"value":[{"id":"b1","displayName":"Madrid"}]}"""
                else -> """{"value":[{"id":"g1","displayName":"Madrid"},{"id":"g2","displayName":"Barcelona"}]}"""
            }
        }
        val catalog = service.locations("token")
        assertEquals(4, requests.size)
        assertEquals(4, catalog.locations.size)
        assertFalse(catalog.isPartial)
        val room = catalog.locations.single { it.matches("aguila MADRID") }
        assertEquals("Sala Águila · Madrid", room.locationName)
        assertEquals(8, room.capacity)
        assertEquals(MeetingLocationKind.Office, catalog.locations.single { it.displayName == "Madrid" }.kind)
    }
    @Test fun optionalPlacesFallbackButRoomPermissionErrorIsNotEmptyCatalog() = runBlocking {
        val service = MeetingGraphService { request ->
            when {
                request.url.encodedPath.endsWith(".building") -> throw MeetingGraphHttpException(404)
                request.url.encodedPath.endsWith(".roomList") -> """{"value":[{"id":"g","displayName":"Madrid"}]}"""
                else -> "{\"value\":[]}"
            }
        }
        val catalog = service.locations("token")
        assertTrue(catalog.isPartial)
        assertEquals(MeetingLocationKind.RoomGroup, catalog.locations.single().kind)
        try { MeetingGraphService { throw MeetingGraphHttpException(403) }.locations("token"); fail("403 must propagate") }
        catch (error: MeetingGraphHttpException) { assertEquals(403, error.status) }
    }
    @Test fun unsafeOrRepeatedPaginationIsRejectedBeforeSendingToken() = runBlocking {
        for (link in listOf("https://evil.example/places", "http://graph.microsoft.com/v1.0/places/microsoft.graph.room",
            "https://graph.microsoft.com:444/v1.0/places/microsoft.graph.room", "https://user:pass@graph.microsoft.com/v1.0/places/microsoft.graph.room",
            "https://graph.microsoft.com/v1.0/users")) {
            var calls = 0
            val service = MeetingGraphService { calls++; """{"value":[],"@odata.nextLink":"$link"}""" }
            try { service.locations("secret"); fail("Unsafe URL must fail") } catch (_: IllegalStateException) { }
            assertEquals(1, calls)
        }
        var calls = 0
        val service = MeetingGraphService { request -> calls++; Gson().toJson(mapOf("value" to emptyList<String>(), "@odata.nextLink" to request.url.toString())) }
        try { service.locations("token"); fail("Pagination loop must fail") } catch (_: IllegalStateException) { }
        assertEquals(1, calls)
    }
}
