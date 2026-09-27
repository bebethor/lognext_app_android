package com.lognext.nexterandroid.features.people

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class PeoplePhonePrivacyTest {
    private fun profile(fields: String): PersonProfileResponse = Gson().fromJson(
        """{"person_code":"test","full_name":"Test Employee",$fields}""",
        PersonProfileResponse::class.java
    )

    @Test fun displaysOnlyCorporatePhoneWhenBothNumbersExist() {
        val result = profile(""""work_phone":"  COMPANY-NUMBER  ","mobile_phone":"PRIVATE-NUMBER"""")
        assertEquals("COMPANY-NUMBER", result.toPeopleRow().workPhone)
    }

    @Test fun personalPhoneIsNeverUsedWhenCorporatePhoneIsMissingNullOrBlank() {
        listOf(
            """"mobile_phone":"PRIVATE-NUMBER"""",
            """"work_phone":null,"mobile_phone":"PRIVATE-NUMBER"""",
            """"work_phone":"","mobile_phone":"PRIVATE-NUMBER"""",
            """"work_phone":"   ","mobile_phone":"PRIVATE-NUMBER""""
        ).forEach { fields ->
            assertEquals("", profile(fields).toPeopleRow().workPhone)
        }
    }

    @Test fun missingCorporatePhoneDoesNotRetainAnOlderNumber() {
        val oldRow = profile(""""work_phone":"OLD-NUMBER"""").toPeopleRow()
        val current = profile(""""mobile_phone":"PRIVATE-NUMBER"""")
        assertEquals("", current.toPeopleRow(fallback = oldRow).workPhone)
    }

    @Test fun currentCorporatePhoneReplacesPreviousNumber() {
        val oldRow = profile(""""work_phone":"OLD-NUMBER"""").toPeopleRow()
        assertEquals("NEW-NUMBER", profile(""""work_phone":"NEW-NUMBER"""").toPeopleRow(oldRow).workPhone)
    }
}
