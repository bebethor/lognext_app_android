package com.lognext.nexterandroid.features.more

import java.net.URI

internal data class WellbeingService private constructor(val id: String, val name: String, val summary: String, val bookingURL: String) {
    companion object {
        fun create(id: String, name: String, summary: String, bookingURL: String): WellbeingService? {
            if (id.isBlank() || name.isBlank()) return null
            val uri = try { URI(bookingURL) } catch (_: Exception) { return null }
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
            return WellbeingService(id, name.trim(), summary, bookingURL)
        }
    }
}
