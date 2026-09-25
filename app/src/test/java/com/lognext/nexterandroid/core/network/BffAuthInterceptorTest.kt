package com.lognext.nexterandroid.core.network

import android.app.Activity
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BffAuthInterceptorTest {
    private class TestAuth(val token: () -> String?) : AuthRepository {
        override val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        override val isLoggingIn = MutableStateFlow(false)
        override suspend fun restoreSession() = Unit
        override suspend fun signIn(activity: Activity) = Unit
        override suspend fun signOut() = Unit
        override suspend fun currentBffToken() = token()
    }

    private fun client(auth: AuthRepository, received: (Request) -> Unit) = OkHttpClient.Builder()
        .addInterceptor(BffAuthInterceptor(auth))
        .addInterceptor { chain ->
            received(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{}".toResponseBody()).build()
        }.build()

    @Test fun usesFreshRepositoryTokenForEachRequest() {
        var calls = 0
        val headers = mutableListOf<String?>()
        val client = client(TestAuth { "token-${++calls}" }) { headers.add(it.header("Authorization")) }
        repeat(2) { client.newCall(Request.Builder().url("https://example.invalid/").build()).execute().close() }
        assertEquals(listOf("Bearer token-1", "Bearer token-2"), headers)
    }

    @Test fun missingOrBlankTokenNeverSendsAnAnonymousRequest() {
        listOf(null, "", " ").forEach { token ->
            var sent = false
            val client = client(TestAuth { token }) { sent = true }
            assertThrows(IOException::class.java) {
                client.newCall(Request.Builder().url("https://example.invalid/").build()).execute()
            }
            assertFalse(sent)
        }
    }

    @Test fun tokenFailureIsReportedWithoutSendingRequest() {
        var sent = false
        val failure = IllegalStateException("interaction required")
        val client = client(TestAuth { throw failure }) { sent = true }
        val error = assertThrows(IOException::class.java) {
            client.newCall(Request.Builder().url("https://example.invalid/").build()).execute()
        }
        assertSame(failure, error.cause)
        assertFalse(sent)
    }
}
