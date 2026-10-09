package com.lognext.nexterandroid.features.people

import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleLeadershipTest {
    @Test fun selectsCommitteeByJobAndKeepsRealIdentifiersForProfileNavigation() = runBlocking {
        val client = object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                assertEquals("", request.url.queryParameter("q"))
                return """{"people":[
                    {"person_code":"real-2","full_name":"Zeta","job_title":"COMITÉ DE DIRECCIÓN","org_unit_name":"STAFF"},
                    {"person_code":"real-1","full_name":"Alfa","job_title":" comite de direccion ","org_unit_name":"STAFF"},
                    {"person_code":"other","full_name":"Otro","job_title":"STAFF","org_unit_name":"COMITÉ DE DIRECCIÓN"},
                    {"person_code":"real-2","full_name":"Zeta","job_title":"COMITÉ DE DIRECCIÓN"},
                    {"person_code":"","job_title":"COMITÉ DE DIRECCIÓN"},
                    {"person_code":"unknown","job_title":null}
                ]}"""
            }
        }
        val rows = PeopleService(client).leadership().map { it.toPeopleRow() }
        assertEquals(listOf("real-1", "real-2"), rows.map { it.personCode })
        assertEquals(listOf("Alfa", "Zeta"), rows.map { it.name })
    }

    @Test fun emptyApiResultDoesNotInventCommitteeMembers() = runBlocking {
        val client = object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange) = """{"people":[]}"""
        }
        assertEquals(emptyList<PersonSummaryResponse>(), PeopleService(client).leadership())
    }
}
