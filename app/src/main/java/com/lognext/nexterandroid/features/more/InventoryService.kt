package com.lognext.nexterandroid.features.more

import com.google.gson.annotations.SerializedName
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

class InventoryService(apiClient: APIClient) : RestService(apiClient) {
    suspend fun myInventory(): InventoryResponse = get("/api/v1/staff/me/inventory", InventoryResponse::class.java)
}

data class InventoryResponse(val items: List<InventoryAssignment> = emptyList())

data class InventoryAssignment(
    val id: String,
    val name: String,
    @SerializedName("item_type") val itemType: String? = null,
    val model: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("asset_number") val assetNumber: String? = null,
    @SerializedName("assigned_from") val assignedFrom: String? = null,
    @SerializedName("assigned_to") val assignedTo: String? = null
) {
    val start: Date? get() = inventoryDate(assignedFrom)
    val end: Date? get() = inventoryDate(assignedTo)
    fun period(today: Date = Date()): InventoryPeriod = when {
        start?.let { startOfDay(it) > startOfDay(today) } == true -> InventoryPeriod.UPCOMING
        end?.let { startOfDay(it) < startOfDay(today) } == true -> InventoryPeriod.HISTORY
        else -> InventoryPeriod.CURRENT
    }
}

enum class InventoryPeriod { CURRENT, UPCOMING, HISTORY }

internal fun inventoryDate(raw: String?): Date? {
    if (raw.isNullOrBlank()) return null
    val ticks = Regex("^/Date\\((-?\\d+)(?:[+-]\\d{4})?\\)/$").matchEntire(raw)?.groupValues?.get(1)
    return runCatching {
        if (ticks != null) Date(ticks.toLong())
        else if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(raw.take(10))) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(raw.take(10))
        } else null
    }.getOrNull()
}

private fun startOfDay(date: Date): Long = Calendar.getInstance().apply {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
