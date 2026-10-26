package com.lognext.nexterandroid.core.network

import android.app.Activity
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.AuthState
import com.lognext.nexterandroid.core.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class MeetingFreshReadTest {
    @get:Rule val temporary = TemporaryFolder()
    private val auth = object : AuthRepository {
        override val authState = MutableStateFlow<AuthState>(AuthState.Authenticated(AuthUser("Ana", "ana@example.com")))
        override val isLoggingIn = MutableStateFlow(false)
        override suspend fun restoreSession() = Unit
        override suspend fun signIn(activity: Activity) = Unit
        override suspend fun signOut() = Unit
        override suspend fun currentBffToken() = "token"
    }

    @Test fun noStoreReadsIgnoreCachedVersionsAndNeverFallBackOffline() {
        val url = "https://example.invalid/api/v1/calendar/events/id"
        val cache = ResponseCache(temporary.newFolder())
        cache.write("ana@example.com", url, "cached-version")
        var offline = false
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            if (offline) throw IOException("offline")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("fresh-version".toResponseBody()).build()
        }.build()
        val client = OkHttpAPIClient(auth, cache, transport)
        val request = Request.Builder().url(url).header("Cache-Control", "no-store").build()
        assertEquals("fresh-version", client.execute(request, 200..200))
        offline = true
        assertThrows(IOException::class.java) { client.execute(request, 200..200) }
        assertEquals("cached-version", cache.read("ana@example.com", url))
    }
}
