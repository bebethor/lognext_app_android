package com.lognext.nexterandroid.features.more

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale

data class MoreVacationBalance(
    @SerializedName("plan_name") val planName: String = "",
    @SerializedName("time_unit") val timeUnit: String = "",
    @SerializedName("total_entitlement") val totalEntitlement: Double = 0.0,
    @SerializedName("total_taken") val totalTaken: Double = 0.0,
    @SerializedName("total_remaining") val totalRemaining: Double = 0.0,
    @SerializedName("effective_from") val effectiveFrom: String? = null,
    @SerializedName("effective_to") val effectiveTo: String? = null,
    @SerializedName("can_request") val canRequest: Boolean = true,
    @SerializedName("allow_overbooking") val allowOverbooking: Boolean = false,
    @SerializedName("requires_reason") val requiresReason: Boolean = false,
    @SerializedName("requires_notes") val requiresNotes: Boolean = false,
    @SerializedName("hide_remaining_on_request") val hideRemainingOnRequest: Boolean = false
)

data class VacationHistoryEntry(
    @SerializedName("event_guid") val eventGuid: String = "",
    @SerializedName("type_name") val typeName: String = "",
    @SerializedName("plan_name") val planName: String = "",
    val status: String = "",
    @SerializedName("effective_from") val effectiveFrom: String? = null,
    @SerializedName("effective_to") val effectiveTo: String? = null,
    @SerializedName("total_days") val totalDays: Double = 0.0,
    val approver: String = "",
    val notes: String = "",
    val reason: String = ""
) {
    val id: String
        get() = eventGuid.ifBlank { "$typeName-$effectiveFrom-$effectiveTo" }

    val formattedPeriod: String
        get() = formatVacationPeriod(effectiveFrom, effectiveTo) ?: "Fechas no disponibles"

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
    @SerializedName("type_guid") val typeGuid: String = "",
    val name: String = "",
    @SerializedName("translated_name") val translatedName: String = "",
    @SerializedName("time_units") val timeUnits: String = "",
    val colour: String = ""
) {
    val id: String
        get() = typeGuid

    val displayName: String
        get() = translatedName.ifBlank { name }

    val colorHex: Long
        get() {
            val normalized = colour.filter { it.isLetterOrDigit() }.let {
                if (it.length == 8) it.takeLast(6) else it
            }
            return normalized.toLongOrNull(16)?.let { 0xFF000000 or it } ?: 0xFFFA3C0F
        }
}

data class VacationAbsencePlan(
    @SerializedName(
        value = "plan_guid",
        alternate = ["plan_id", "guid", "id", "planGuid"]
    ) val planGuid: String = "",
    @SerializedName(
        value = "plan_name",
        alternate = ["name", "translated_name", "display_name", "planName"]
    ) val name: String = "",
    @SerializedName(
        value = "category",
        alternate = ["absence_category", "absenceCategory"]
    ) val category: String = "",
    @SerializedName(
        value = "time_unit",
        alternate = ["time_units", "timeUnit", "timeUnits"]
    ) val timeUnit: String = ""
) {
    val id: String
        get() = planGuid.ifBlank { "$category-$name" }

    val displayName: String
        get() = name.ifBlank { "Plan de ausencia" }

    val categoryForRequest: String?
        get() = category.ifBlank { null }
}

data class VacationHistoryResponse(
    val entries: List<VacationHistoryEntry> = emptyList()
)

data class VacationTypesResponse(
    val types: List<VacationRequestType> = emptyList()
)

data class VacationPlansResponse(
    @SerializedName(
        value = "plans",
        alternate = ["absence_plans", "items"]
    ) val plans: List<VacationAbsencePlan> = emptyList()
)

data class VacationCreateRequest(
    @SerializedName("type_guid") val typeGuid: String,
    @SerializedName("effective_from") val effectiveFrom: String,
    @SerializedName("effective_to") val effectiveTo: String,
    val notes: String,
    val reason: String,
    val category: String?
)

data class VacationCreateResponse(
    @SerializedName("event_guid") val eventGuid: String = "",
    val inserted: Boolean = false,
    val messages: String = ""
)

data class ClockNotificationSetting(
    val id: String,
    val title: String,
    val subtitle: String,
    val hasTime: Boolean,
    val defaultMinutes: Int
)

fun formatVacationPeriod(start: String?, end: String?): String? {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val startDate = start?.takeIf { it.isNotBlank() }?.let { runCatching { parser.parse(it) }.getOrNull() }
    val endDate = end?.takeIf { it.isNotBlank() }?.let { runCatching { parser.parse(it) }.getOrNull() }
    val first = startDate ?: endDate ?: return null
    if (endDate == null || startDate == null || startDate == endDate) {
        return formatter.format(first).replace(".", "")
    }
    return "${formatter.format(startDate).replace(".", "")} – ${formatter.format(endDate).replace(".", "")}"
}
