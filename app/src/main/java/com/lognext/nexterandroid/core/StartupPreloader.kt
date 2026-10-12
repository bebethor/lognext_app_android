package com.lognext.nexterandroid.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Warm the shared cache without making startup depend on any individual resource. */
internal class StartupPreloader(
    private val primary: List<suspend () -> Unit>,
    private val secondary: List<suspend () -> Unit>
) {
    suspend fun preload() {
        load(primary)
        load(secondary)
    }

    private suspend fun load(requests: List<suspend () -> Unit>) = coroutineScope {
        requests.forEach { request ->
            launch {
                try {
                    request()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    // The screen retains its own retry/error handling. Do not fail sibling loads.
                }
            }
        }
    }
}
