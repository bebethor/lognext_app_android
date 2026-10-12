package com.lognext.nexterandroid.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class StartupPreloaderTest {
    @Test fun primaryLoadsRunTogetherBeforeSecondaryAndFailuresAreIsolated() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val started = mutableSetOf<String>()
        val completed = mutableSetOf<String>()
        val preloader = StartupPreloader(
            primary = listOf(
                { started.add("tasks"); release.await(); completed.add("tasks"); Unit },
                { started.add("meetings"); throw IllegalStateException("Offline") }
            ),
            secondary = listOf({ started.add("team"); completed.add("team"); Unit })
        )
        val job = launch { preloader.preload() }
        yield(); yield()
        assertEquals(setOf("tasks", "meetings"), started)
        assertFalse(started.contains("team"))
        release.complete(Unit)
        job.join()
        assertEquals(setOf("tasks", "team"), completed)
    }

    @Test fun cancellationDoesNotStartSecondaryRequests() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        var secondaryStarted = false
        val preloader = StartupPreloader(
            primary = listOf({ started.complete(Unit); never.await() }),
            secondary = listOf({ secondaryStarted = true })
        )
        val job = launch { preloader.preload() }
        started.await()
        job.cancel(CancellationException("Signed out"))
        job.join()
        assertFalse(secondaryStarted)
    }
}
