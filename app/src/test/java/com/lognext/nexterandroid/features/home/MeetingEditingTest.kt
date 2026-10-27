package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class MeetingEditingTest {
    private fun event() = HomeCalendarEvent(
        "id/with?query", "Planning", "2026-10-26T09:00:00Z", "2026-10-26T10:00:00Z", "Madrid", true,
        "https://teams.microsoft.com/meeting", 1, "Organizer", listOf(MeetingAttendee("Ana", "ana@example.com")),
        RoomReservation("room@example.com", "Room", "accepted"), true, "occurrence", "version-1"
    )

    @Test fun unchangedFieldsAreOmittedAndTeamsBodyNeverReplaced() {
        val source = event()
        val changes = meetingEditChanges(source, source.subject, source.startDate!!, source.endDate!!,
            source.location!!, source.attendees!!, MeetingRoom("room@example.com", "Room"), false)
        assertEquals(0, changes.size())
        val title = meetingEditChanges(source, "Changed", source.startDate!!, source.endDate!!,
            source.location!!, source.attendees!!, null, false)
        assertEquals(setOf("subject"), title.keySet())
        assertFalse(title.has("body"))
        assertFalse(title.has("is_online_meeting"))
    }

    @Test fun removingRoomSendsExplicitNullAndAttendeeChangesAreSeparate() {
        val source = event()
        val changes = meetingEditChanges(source, source.subject, source.startDate!!, source.endDate!!,
            "Office", emptyList(), null, true)
        assertTrue(changes["room"].isJsonNull)
        assertEquals("Office", changes["location"].asString)
        assertEquals(0, changes["attendees"].asJsonArray.size())
    }

    @Test fun missingPermissionFieldsNeverMakeOldResponsesEditable() {
        val json = """{"id":"id","subject":"Read only","start":"2026-10-26T09:00:00Z","end":"2026-10-26T10:00:00Z"}"""
        val decoded = Gson().fromJson(json, HomeCalendarEvent::class.java)
        assertFalse(decoded.canEdit)
        assertNull(decoded.etag)
    }

    @Test fun detailBypassesCacheAndTreatsOutlookIdAsOneSegment() = runBlocking {
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                assertEquals("GET", request.method)
                assertEquals("no-store", request.header("Cache-Control"))
                assertTrue(request.url.encodedPath.endsWith("id%2Fwith%3Fquery"))
                return Gson().toJson(event())
            }
        })
        assertTrue(service.getMeeting(event().id).canEdit)
    }

    @Test fun updateUsesPatchAndVersionAndReturnsAuthoritativeMeeting() = runBlocking {
        val service = HomeService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                assertEquals("PATCH", request.method)
                assertEquals(200..200, acceptedStatusCodes)
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val body = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                assertEquals(setOf("etag", "subject"), body.keySet())
                assertEquals("version-1", body["etag"].asString)
                assertTrue(request.url.encodedPath.endsWith("id%2Fwith%3Fquery"))
                return Gson().toJson(event().copy(subject = "Updated", etag = "version-2"))
            }
        })
        val updated = service.updateMeeting(event().id, JsonObject().apply {
            addProperty("etag", "version-1"); addProperty("subject", "Updated")
        })
        assertEquals("Updated", updated.subject)
        assertEquals("version-2", updated.etag)
    }
}
