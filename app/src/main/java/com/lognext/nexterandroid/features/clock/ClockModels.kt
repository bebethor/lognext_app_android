package com.lognext.nexterandroid.features.clock

import java.util.Date
import java.util.UUID

data class ClockEntry(
    val clockIn: Date?,
    val clockOut: Date?,
    val recordId: String = UUID.randomUUID().toString()
)

enum class ClockTimelineKind {
    Entry,
    Exit
}

data class ClockTimelineRow(
    val kind: ClockTimelineKind,
    val date: Date,
    val recordId: String
) {
    val id: String = "$recordId-$kind-${date.time}"
}

data class RecentHistoryRow(
    val dayKey: String,
    val dateText: String,
    val entryText: String,
    val exitText: String,
    val totalText: String
)
