package com.lognext.nexterandroid.features.more

data class MoreVacationBalance(
    val planName: String,
    val timeUnit: String,
    val totalEntitlement: Double,
    val totalTaken: Double,
    val totalRemaining: Double,
    val canRequest: Boolean,
    val allowOverbooking: Boolean,
    val requiresReason: Boolean,
    val requiresNotes: Boolean,
    val hideRemainingOnRequest: Boolean
)

data class VacationHistoryEntry(
    val id: String,
    val typeName: String,
    val status: String,
    val formattedPeriod: String,
    val totalDays: Double,
    val approver: String
) {
    val localizedStatus: String
        get() = when (status.lowercase()) {
            "approved", "aprobado", "accepted" -> "Aprobada"
            "rejected", "rechazado" -> "Rechazada"
            "cancelled", "canceled", "cancelado" -> "Cancelada"
            "pending", "submitted", "approval pending", "pending approval", "aprobación pendiente" -> "Pendiente"
            else -> status
        }
}

data class VacationRequestType(
    val id: String,
    val displayName: String,
    val timeUnits: String,
    val colorHex: Long
)

data class ClockNotificationSetting(
    val id: String,
    val title: String,
    val subtitle: String,
    val hasTime: Boolean,
    val defaultMinutes: Int
)
