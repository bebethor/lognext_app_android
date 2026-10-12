package com.lognext.nexterandroid.core.network

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException

/** Shares only in-progress requests. Completed and failed requests are removed. */
internal class SingleFlight<T> {
    private val requests = ConcurrentHashMap<String, CompletableFuture<T>>()

    fun execute(key: String, operation: () -> T): T {
        val result = CompletableFuture<T>()
        val existing = requests.putIfAbsent(key, result)
        if (existing != null) {
            try {
                return existing.get()
            } catch (error: ExecutionException) {
                throw error.cause ?: error
            }
        }
        try {
            return operation().also { result.complete(it) }
        } catch (error: Throwable) {
            result.completeExceptionally(error)
            throw error
        } finally {
            requests.remove(key, result)
        }
    }
}
