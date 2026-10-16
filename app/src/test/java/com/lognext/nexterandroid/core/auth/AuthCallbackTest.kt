package com.lognext.nexterandroid.core.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthCallbackTest {
    @Test fun missingMicrosoftCallbackTimesOutAndAnotherAttemptCanSucceed() = runBlocking {
        var callback: ((Result<String>) -> Unit)? = null
        try {
            awaitAuthCallback<String>(50) { callback = it }
            fail("Missing callback must not keep login loading")
        } catch (_: TimeoutCancellationException) {
            // The expired callback cannot complete the next attempt.
        }
        callback!!(Result.success("expired result"))
        assertEquals("new session", awaitAuthCallback<String> { it(Result.success("new session")) })
    }

    @Test fun cancellingTheScreenDiscardsLateCallback() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var callback: ((Result<String>) -> Unit)? = null
        var completed = false
        val attempt = launch {
            awaitAuthCallback<String> { callback = it; started.complete(Unit) }
            completed = true
        }
        started.await()
        attempt.cancel()
        attempt.join()
        callback!!(Result.success("old session"))
        assertFalse(completed)
    }

    @Test fun duplicateCallbacksOnlyCompleteOnce() = runBlocking {
        assertEquals("first", awaitAuthCallback<String> {
            it(Result.success("first"))
            it(Result.failure(IllegalStateException("late error")))
        })
    }

    @Test fun microsoftFailureIsReturnedInsteadOfLeavingTheWaitPending() = runBlocking {
        val expected = IllegalStateException("Microsoft unavailable")
        try {
            awaitAuthCallback<String> { it(Result.failure(expected)) }
            fail("Failure must reach the login screen")
        } catch (actual: IllegalStateException) {
            assertEquals(expected.message, actual.message)
        }
    }
}
