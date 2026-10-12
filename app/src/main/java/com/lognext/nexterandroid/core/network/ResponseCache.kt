package com.lognext.nexterandroid.core.network

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Account-scoped response cache. Authentication tokens are never stored here. */
class ResponseCache(private val root: File) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, "api-cache"))
    private val staleUrls = mutableSetOf<String>()
    private val mutableHasStaleData = kotlinx.coroutines.flow.MutableStateFlow(false)
    val hasStaleData: kotlinx.coroutines.flow.StateFlow<Boolean> = mutableHasStaleData
    @Synchronized fun markStale(url: String, stale: Boolean) {
        if (stale) staleUrls.add(url) else staleUrls.remove(url)
        mutableHasStaleData.value = staleUrls.isNotEmpty()
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun directory(account: String) = File(root, hash(account))
    @Synchronized fun read(account: String, url: String, maxAge: Long? = 120000): String? {
        val file = File(directory(account), hash(url))
        if (!file.exists() || (maxAge != null && System.currentTimeMillis() - file.lastModified() > maxAge)) return null
        return runCatching { file.readText() }.getOrNull()
    }
    @Synchronized fun write(account: String, url: String, body: String) {
        val directory = directory(account)
        directory.mkdirs()
        val file = File(directory, hash(url))
        val temp = File(directory, hash(url) + ".tmp")
        temp.writeText(body)
        temp.renameTo(file)
    }
    @Synchronized fun invalidate(account: String) { directory(account).listFiles()?.forEach { it.setLastModified(1) } }
    @Synchronized fun clear(account: String) { directory(account).deleteRecursively(); staleUrls.clear(); mutableHasStaleData.value = false }
}
