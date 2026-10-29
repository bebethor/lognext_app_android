package com.lognext.nexterandroid.features.notifications

/** Keep account and subscription checks identical for notification display and navigation. */
internal object AbsencePushPolicy {
    fun accepts(optedIn: Boolean, expected: String?, received: String?): Boolean =
        optedIn && !expected.isNullOrBlank() && received == expected

    fun shouldRenew(lastKey: String?, key: String, renewedAt: Long, now: Long): Boolean =
        key != lastKey || now < renewedAt || now - renewedAt >= 86_400_000
}
