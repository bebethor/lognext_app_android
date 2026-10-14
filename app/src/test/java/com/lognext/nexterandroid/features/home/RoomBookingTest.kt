package com.lognext.nexterandroid.features.home

import com.google.gson.JsonParser
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class RoomBookingTest {
    private fun request() = CreateMeetingRequest("Test", "2026-10-14T10:00:00Z", "2026-10-14T11:00:00Z",
        "", "Sala Azul", false, listOf(MeetingAttendee("Ana", "ana@example.com")), "transaction", MeetingRoom("room@example.com", "Sala Azul"))

    @Test fun oldBackendCannotSilentlyIgnoreRoomBooking() = runBlocking {
        var creates = 0
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                if (request.method == "POST") creates++
                return "{}"
            }
        })
        try { service.createMeeting(request()); fail("Booking must fail before creating") }
        catch (_: RoomBookingUnavailableException) { }
        assertEquals(0, creates)
    }
    @Test fun resourceEmailIsSentAndPendingStatusIsPreserved() = runBlocking {
        var checks = 0
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                if (request.method == "GET") {
                    checks++
                    assertEquals("/api/v1/calendar/booking-capabilities", request.url.encodedPath)
                    return "{\"resource_booking\":true,\"version\":1}"
                }
                val buffer = Buffer(); request.body!!.writeTo(buffer)
                val body = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                assertEquals("room@example.com", body["room"].asJsonObject["email"].asString)
                assertEquals(1, body["attendees"].asJsonArray.size())
                return """{"id":"event","subject":"Test","start":"2026-10-14T10:00:00Z","end":"2026-10-14T11:00:00Z","is_online_meeting":false,"room_reservation":{"email":"room@example.com","name":"Sala Azul","status":"pending"}}"""
            }
        })
        assertEquals("pending", service.createMeeting(request()).roomReservation!!.status)
        assertEquals(1, checks)
    }
    @Test fun neitherTeamsNorLocationNeverCreatesMeeting() = runBlocking {
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String { fail("No network expected"); return "" }
        })
        try { service.createMeeting(request().copy(location = "  ", room = null)); fail("Missing location") }
        catch (_: IllegalArgumentException) { }
    }
}
