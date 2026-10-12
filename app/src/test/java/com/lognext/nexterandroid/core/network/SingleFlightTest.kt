package com.lognext.nexterandroid.core.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class SingleFlightTest {
    @Test fun simultaneousRequestsShareOneOperation() {
        val requests = SingleFlight<String>()
        val count = AtomicInteger()
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = (1..8).map {
                pool.submit<String> {
                    ready.countDown(); start.await()
                    requests.execute("account|tasks") { count.incrementAndGet(); release.await(); "tasks" }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            // Let all callers reach the shared request before releasing the owner.
            Thread.sleep(100)
            release.countDown()
            assertEquals(List(8) { "tasks" }, results.map { it.get(5, TimeUnit.SECONDS) })
            assertEquals(1, count.get())
            assertEquals("fresh", requests.execute("account|tasks") { "fresh" })
        } finally { release.countDown(); pool.shutdownNow() }
    }

    @Test fun aFailedOperationCanBeRetried() {
        val requests = SingleFlight<String>()
        try { requests.execute("tasks") { throw IllegalStateException("offline") }; fail() }
        catch (_: IllegalStateException) { }
        assertEquals("recovered", requests.execute("tasks") { "recovered" })
    }

    @Test fun differentAccountsAreIndependent() {
        val requests = SingleFlight<String>()
        assertEquals("ana", requests.execute("ana|tasks") { "ana" })
        assertEquals("bob", requests.execute("bob|tasks") { "bob" })
    }
}
