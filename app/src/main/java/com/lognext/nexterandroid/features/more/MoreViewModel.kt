package com.lognext.nexterandroid.features.more

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MoreViewModel : ViewModel() {
    val balance = MoreVacationBalance(
        planName = "Vacaciones anuales",
        timeUnit = "días",
        totalEntitlement = 23.0,
        totalTaken = 8.0,
        totalRemaining = 15.0,
        canRequest = true,
        allowOverbooking = false,
        requiresReason = false,
        requiresNotes = false,
        hideRemainingOnRequest = false
    )

    val historyItems = listOf(
        VacationHistoryEntry(
            id = "vac-1",
            typeName = "Vacaciones",
            status = "approved",
            formattedPeriod = "15 jul 2026 – 19 jul 2026",
            totalDays = 5.0,
            approver = "Miguel Ángel Saiz"
        ),
        VacationHistoryEntry(
            id = "vac-2",
            typeName = "Vacaciones",
            status = "pending",
            formattedPeriod = "12 ago 2026 – 14 ago 2026",
            totalDays = 3.0,
            approver = "Miguel Ángel Saiz"
        ),
        VacationHistoryEntry(
            id = "vac-3",
            typeName = "Festivo",
            status = "approved",
            formattedPeriod = "3 jun 2026",
            totalDays = 1.0,
            approver = "Miguel Ángel Saiz"
        )
    )

    val requestTypes = listOf(
        VacationRequestType("type-holiday", "Festivo", "days", 0xFFFA3C0F),
        VacationRequestType("type-non-working", "No laborable", "days", 0xFF3791F5),
        VacationRequestType("type-festivity", "Festividad", "days", 0xFF3CE6E6),
        VacationRequestType("type-vacation", "Vacaciones", "days", 0xFFC896FF)
    )

    val notificationSettings = listOf(
        ClockNotificationSetting("workStart", "Recordar fichar entrada", "Al empezar a trabajar.", true, 9 * 60),
        ClockNotificationSetting("lunchStart", "Salida para comer", "Cuando te vayas a comer.", true, 14 * 60),
        ClockNotificationSetting("lunchEnd", "Entrada después de comer", "Cuando vuelvas de comer.", true, 15 * 60),
        ClockNotificationSetting("workEnd", "Salida fin de jornada", "Cuando termines de trabajar.", true, 18 * 60),
        ClockNotificationSetting("longOpenEntry", "Jornada abierta muchas horas", "Aviso si pasan 9 horas desde la entrada.", false, 0),
        ClockNotificationSetting("noRecordEndOfDay", "Sin registros al final del día", "Aviso a las 19:00 si no has fichado nada.", false, 0)
    )

    val notificationEnabled = mutableStateMapOf<String, Boolean>().apply {
        notificationSettings.forEach { put(it.id, false) }
    }
    val notificationMinutes = mutableStateMapOf<String, Int>().apply {
        notificationSettings.forEach { put(it.id, it.defaultMinutes) }
    }

    var showVacationRequest by mutableStateOf(false)
    var showConfirmation by mutableStateOf(false)
    var confirmationTitle by mutableStateOf("Solicitud creada")
    var confirmationMessage by mutableStateOf("Solicitud enviada correctamente.")
    var selectedTypeId by mutableStateOf(requestTypes.first().id)
    var startDate by mutableStateOf("2026-08-12")
    var endDate by mutableStateOf("2026-08-14")
    var reason by mutableStateOf("")
    var notes by mutableStateOf("")

    val canSubmit: Boolean
        get() = selectedTypeId.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()

    fun ensureEndDateAfterStart() {
        if (endDate < startDate) {
            endDate = startDate
        }
    }

    fun submitVacationRequest() {
        showVacationRequest = false
        confirmationTitle = "Solicitud creada"
        confirmationMessage = "Solicitud enviada correctamente."
        showConfirmation = true
    }
}
