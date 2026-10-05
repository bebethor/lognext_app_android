package com.lognext.nexterandroid.features.people

import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleSearchQueryTest {
    @Test fun emptySearchIsSentAsAnExplicitQueryParameter() = runBlocking {
        var captured: Request? = null
        val service = PeopleService(object : APIClient {
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                captured = request
                return """{"query":"","people":[]}"""
            }
        })
        service.searchStaff("")
        assertEquals("/api/v1/staff/search", captured!!.url.encodedPath)
        assertEquals("", captured!!.url.queryParameter("q"))
    }
}
