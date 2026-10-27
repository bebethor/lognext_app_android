package com.lognext.nexterandroid.features.home

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class MeetingSummaryTest {
    private fun event(extra: String = "") = Gson().fromJson("""{
        "id":"event","subject":"Planning","start":"2026-10-26T13:00:00+02:00","end":"2026-10-26T12:30:00Z",
        "is_online_meeting":true,"location":"Physical room","attendees_count":3,"organizer":"Ana"$extra
    }""", HomeCalendarEvent::class.java)

    @Test fun homeSummaryUsesDurationAndCountWithoutRepeatingModeOrRoom() {
        val event = event()
        assertEquals("1h 30min · 3 asistentes", event.subtitle)
        assertEquals("Online", event.tagText)
        assertFalse(event.subtitle.contains("Physical room"))
        assertFalse(event.subtitle.contains("Teams"))
        assertEquals("1h 30min · 0 asistentes", event.copy(attendeesCount = 0).subtitle)
        assertEquals("Presencial", event.copy(isOnlineMeeting = false, location = null).tagText)
    }

    @Test fun organizerComesFromOutlookIdentityNotDisplayName() {
        assertTrue(event(",\"is_organizer\":true,\"can_edit\":false").organizerIsMe)
        assertFalse(event(",\"is_organizer\":false,\"can_edit\":true").organizerIsMe)
        assertFalse(event().organizerIsMe)
    }

    @Test fun olderBackendCanIdentifyEditableOwnMeetingsWithoutGuessingOtherNames() {
        assertTrue(event(",\"can_edit\":true").organizerIsMe)
        assertFalse(event(",\"can_edit\":false").organizerIsMe)
    }
}
