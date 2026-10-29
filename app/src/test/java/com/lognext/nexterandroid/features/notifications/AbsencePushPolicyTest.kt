package com.lognext.nexterandroid.features.notifications

import org.junit.Assert.*
import org.junit.Test

class AbsencePushPolicyTest {
    @Test fun ignoresOldAccountAndOptOutEvenWhenGoogleDeliversLate() {
        assertTrue(AbsencePushPolicy.accepts(true, "current", "current"))
        assertFalse(AbsencePushPolicy.accepts(false, "current", "current"))
        assertFalse(AbsencePushPolicy.accepts(true, "current", "previous-account"))
        assertFalse(AbsencePushPolicy.accepts(true, null, null))
        assertFalse(AbsencePushPolicy.accepts(true, "", ""))
    }
    @Test fun renewsOnLaunchTokenOrAccountChangeAndAfterOneDay() {
        val day = 86_400_000L
        assertTrue(AbsencePushPolicy.shouldRenew(null, "account|token", day, day))
        assertFalse(AbsencePushPolicy.shouldRenew("account|token", "account|token", day, day + 60_000))
        assertTrue(AbsencePushPolicy.shouldRenew("account|old", "account|new", day, day + 60_000))
        assertTrue(AbsencePushPolicy.shouldRenew("old|token", "new|token", day, day + 60_000))
        assertTrue(AbsencePushPolicy.shouldRenew("account|token", "account|token", day, 2 * day))
        assertTrue(AbsencePushPolicy.shouldRenew("account|token", "account|token", day, day - 1))
    }
}
