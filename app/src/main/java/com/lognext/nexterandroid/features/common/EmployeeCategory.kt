package com.lognext.nexterandroid.features.common

import java.util.Locale

enum class EmployeeCategory(val badge: String) {
    Staff("STF"),
    Consultant("CON");

    companion object {
        fun fromJobTitle(jobTitle: String?): EmployeeCategory? = when (jobTitle?.trim()?.uppercase(Locale.ROOT)) {
            "STAFF" -> Staff
            "CONSULTOR" -> Consultant
            else -> null
        }
    }
}
