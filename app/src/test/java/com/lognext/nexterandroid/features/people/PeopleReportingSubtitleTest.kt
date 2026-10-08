package com.lognext.nexterandroid.features.people

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleReportingSubtitleTest {
    @Test fun internUnitTakesPrecedenceOverStaffJobCategory() {
        val person = PersonSummaryResponse(
            personCode = "test", fullName = "Persona de prueba",
            jobTitle = "STAFF", orgUnitName = "BECARIOS"
        ).toPeopleRow()
        assertEquals("BECARIOS", person.reportingSubtitle)
        assertEquals("STAFF", person.role)
    }

    @Test fun missingUnitFallsBackToJobCategory() {
        listOf(null, "", "  ").forEach { unit ->
            val person = PersonSummaryResponse(jobTitle = "CONSULTOR", orgUnitName = unit).toPeopleRow()
            assertEquals("CONSULTOR", person.reportingSubtitle)
        }
    }
}
