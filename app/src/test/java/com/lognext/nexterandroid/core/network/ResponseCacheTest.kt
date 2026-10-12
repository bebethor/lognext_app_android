package com.lognext.nexterandroid.core.network

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class ResponseCacheTest {
    @Test fun cachePersistsAndNeverCrossesAccounts() {
        val root = Files.createTempDirectory("response-cache").toFile()
        try {
            val first = ResponseCache(root)
            first.write("ana", "https://example.com/tasks", "ana tasks")
            val restored = ResponseCache(root)
            assertEquals("ana tasks", restored.read("ana", "https://example.com/tasks"))
            assertNull(restored.read("bob", "https://example.com/tasks"))
            restored.invalidate("ana")
            assertNull(restored.read("ana", "https://example.com/tasks"))
            assertEquals("ana tasks", restored.read("ana", "https://example.com/tasks", maxAge = null))
            restored.clear("ana")
            assertNull(restored.read("ana", "https://example.com/tasks", maxAge = null))
        } finally { root.deleteRecursively() }
    }

    @Test fun staleWarningRemainsUntilAllFailedResourcesRecover() {
        val root = Files.createTempDirectory("response-cache").toFile()
        try {
            val cache = ResponseCache(root)
            cache.markStale("tasks", true)
            cache.markStale("meetings", true)
            cache.markStale("tasks", false)
            assertTrue(cache.hasStaleData.value)
            cache.markStale("meetings", false)
            assertFalse(cache.hasStaleData.value)
        } finally { root.deleteRecursively() }
    }
}
