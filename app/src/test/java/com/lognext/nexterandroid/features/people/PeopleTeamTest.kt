package com.lognext.nexterandroid.features.people

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleTeamTest {
    private val me = PersonProfileResponse(
        personCode = "employee", fullName = "Employee", orgUnitName = "Staff"
    )
    private val colleague = PersonSummaryResponse(
        personCode = "colleague", fullName = "Colleague", orgUnitName = "Becarios"
    )

    @Test fun includesCurrentUserAlongsideReportsFromAnotherUnit() {
        val result = buildPeopleTeam(me, TeamResponse(members = listOf(colleague)))
        assertEquals("Staff", result.title)
        assertEquals(listOf("employee", "colleague"), result.people.map { it.personCode })
        assertTrue(result.people.first().isCurrentUser)
        assertEquals("Becarios", result.people.last().orgUnitName)
    }

    @Test fun retainsOwnProfileWithoutDuplicatesWhenTeamAlreadyIncludesUser() {
        val summary = PersonSummaryResponse(personCode = "employee", fullName = "Old name")
        val result = buildPeopleTeam(me, TeamResponse(members = listOf(colleague, summary, colleague)))
        assertEquals(2, result.people.size)
        assertEquals("Employee", result.people.first().name)
        assertEquals(1, result.people.count { it.isCurrentUser })
    }

    @Test fun emptyTeamStillShowsCurrentUserAndUnit() {
        val result = buildPeopleTeam(me, TeamResponse())
        assertEquals("Staff", result.title)
        assertEquals(listOf(me.toPeopleRow(isCurrentUser = true)), result.people)
    }

    @Test fun missingProfileUnitFallsBackToTeamMetadataAndNeverToFirstMember() {
        val team = TeamResponse(OrgUnitResponse(orgUnitName = "Staff"), listOf(colleague))
        listOf(null, "", "  ").forEach { name ->
            assertEquals("Staff", buildPeopleTeam(me.copy(orgUnitName = name), team).title)
            assertEquals("Mi equipo", buildPeopleTeam(me.copy(orgUnitName = name), team.copy(orgUnit = null)).title)
        }
    }
}
