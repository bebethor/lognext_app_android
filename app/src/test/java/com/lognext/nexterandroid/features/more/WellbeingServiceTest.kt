package com.lognext.nexterandroid.features.more

import org.junit.Assert.*
import org.junit.Test

class WellbeingServiceTest {
    @Test fun acceptsPublishedHttpsBookingPage() {
        val service = WellbeingService.create("service", " Service ", "", "https://example.com/book")
        assertEquals("Service", service?.name)
    }
    @Test fun rejectsInvalidAndInsecureLinks() {
        listOf("http://example.com", "https://user:password@example.com/book", "not-a-url", "javascript:alert(1)", "https:///book").forEach {
            assertNull(WellbeingService.create("service", "Service", "", it))
        }
    }
    @Test fun rejectsMissingServiceIdentity() {
        assertNull(WellbeingService.create("", "Service", "", "https://example.com"))
        assertNull(WellbeingService.create("service", "  ", "", "https://example.com"))
    }
}
