package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class HomeParityTest {
    @Test fun todayIncludesMoreThanThreeAndFinishedMeetings() {
        val events = (1..6).map { HomeCalendarEvent("$it", "Meeting", "2020-10-12T08:00:00Z", "2020-10-12T09:00:00Z", null, false, null, 0, null) }
        val state = HomeUiState(todayMeetings = events, agendaEvents = events)
        assertEquals(6, state.visibleMeetings.size)
        assertEquals(6, state.visibleAgendaEvents.size)
        assertTrue(state.copy(agendaScope = AgendaScope.Week).visibleAgendaEvents.isEmpty())
    }

    @Test fun tasksSortByCreationInsteadOfDueDateAndUnknownDatesLast() {
        val response = Gson().fromJson("""{"tasks":[
            {"id":"older","title":"Old","created_date":"2026-10-11T10:00:00Z","due_date":"2026-10-20T00:00:00Z"},
            {"id":"missing","title":"Unknown"},
            {"id":"newer","title":"New","created_date":"2026-10-12T10:00:00.123456Z","due_date":"2026-10-13T00:00:00Z"}
        ]}""", HomeTaskListResponse::class.java)
        assertEquals(listOf("newer", "older", "missing"), response.tasks.sortedByCreationDate().map { it.id })
        val edited = response.tasks.first().copy(title = "Edited", dueDate = null)
        assertEquals(response.tasks.first().creationDate, edited.creationDate)
    }

    @Test fun editingSendsExplicitNullAndDoesNotChangeCreationOrCompletion() = runBlocking {
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                val buffer = Buffer(); request.body!!.writeTo(buffer)
                val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                assertEquals("PATCH", request.method)
                assertEquals("New title", json["title"].asString)
                assertTrue(json["due_date"].isJsonNull)
                assertFalse(json.has("created_date"))
                assertFalse(json.has("is_completed"))
                return ""
            }
        })
        service.editTask("task", "New title", "Description", "high", null)
    }

    @Test fun meetingCreationForwardsAttendeesAndStableTransactionId() = runBlocking {
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                val buffer = Buffer(); request.body!!.writeTo(buffer)
                val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                assertEquals("/api/v1/calendar/events", request.url.encodedPath)
                assertEquals("retry-id", json["transaction_id"].asString)
                assertTrue(json["is_online_meeting"].asBoolean)
                assertEquals("ana@example.com", json["attendees"].asJsonArray[0].asJsonObject["email"].asString)
                return """{"id":"meeting","subject":"Test","start":"2026-10-12T10:00:00Z","end":"2026-10-12T11:00:00Z","is_online_meeting":true}"""
            }
        })
        service.createMeeting(CreateMeetingRequest("Test", "2026-10-12T10:00:00Z", "2026-10-12T11:00:00Z", "", "", true, listOf(MeetingAttendee("Ana", "ana@example.com")), "retry-id"))
        Unit
    }
}
