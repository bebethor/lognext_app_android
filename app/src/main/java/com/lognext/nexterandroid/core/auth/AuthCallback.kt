package com.lognext.nexterandroid.core.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/** Bound callback-based MSAL waits; late callbacks cannot change a finished attempt. */
internal suspend fun <T> awaitAuthCallback(
    timeoutMillis: Long = 30_000,
    start: (complete: (Result<T>) -> Unit) -> Unit
): T = withTimeout(timeoutMillis) {
    val result = CompletableDeferred<T>()
    try {
        start { response ->
            response.fold(result::complete, result::completeExceptionally)
        }
        result.await()
    } finally {
        result.cancel()
    }
}
