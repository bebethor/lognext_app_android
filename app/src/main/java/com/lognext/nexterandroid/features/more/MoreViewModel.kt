package com.lognext.nexterandroid.features.more

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.network.APIError
import com.lognext.nexterandroid.features.clock.ClockService
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MoreViewModel(
    private val service: VacationService,
    private val clockService: ClockService
) : ViewModel() {
    var balance by mutableStateOf(MoreVacationBalance(canRequest = false))
        private set
    var plans by mutableStateOf<List<VacationAbsencePlan>>(emptyList())
        private set
    var historyItems by mutableStateOf<List<VacationHistoryEntry>>(emptyList())
        private set
    var requestTypes by mutableStateOf<List<VacationRequestType>>(emptyList())
        private set

    var isLoadingBalance by mutableStateOf(false)
        private set
    var hasLoadedBalance by mutableStateOf(false)
        private set
    var balanceErrorMessage by mutableStateOf<String?>(null)
        private set
    var planInfoMessage by mutableStateOf<String?>(null)
        private set
    var isLoadingHistory by mutableStateOf(false)
        private set
    var hasLoadedHistory by mutableStateOf(false)
        private set
    var historyErrorMessage by mutableStateOf<String?>(null)
        private set
    var isLoadingRequestData by mutableStateOf(false)
        private set
    var requestErrorMessage by mutableStateOf<String?>(null)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var cancellingEventGuid by mutableStateOf<String?>(null)
        private set
    var cancellationErrorMessage by mutableStateOf<String?>(null)
        private set
    var cancellationSuccessMessage by mutableStateOf<String?>(null)
        private set

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
    var selectedTypeId by mutableStateOf("")
    var selectedPlanId by mutableStateOf("")
    var startDate by mutableStateOf(todayString())
    var endDate by mutableStateOf(todayString())
    var reason by mutableStateOf("")
    var notes by mutableStateOf("")
    var vacationEntryToCancel by mutableStateOf<VacationHistoryEntry?>(null)

    private var hasLoadedPlans = false
    private var hasLoadedTypes = false
    private var hasStarted = false

    val canSubmit: Boolean
        get() = !isSubmitting &&
            !isLoadingRequestData &&
            requestTypes.isNotEmpty() &&
            plans.isNotEmpty() &&
            selectedTypeId.isNotBlank() &&
            selectedPlan != null &&
            endDate >= startDate &&
            balance.canRequest &&
            (!balance.requiresReason || reason.trim().isNotEmpty()) &&
            (!balance.requiresNotes || notes.trim().isNotEmpty())

    val selectedPlan: VacationAbsencePlan?
        get() = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()

    fun selectPlan(plan: VacationAbsencePlan) {
        selectedPlanId = plan.id
        selectedTypeId = selectedTypeId.ifBlank { requestTypes.firstOrNull()?.id.orEmpty() }
    }

    fun loadIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        refresh()
    }

    fun refresh() {
        loadBalance(force = true)
        loadPlans(force = true)
        loadHistory(force = true)
    }

    fun loadBalance(force: Boolean = false) {
        if (isLoadingBalance || (hasLoadedBalance && !force)) return
        viewModelScope.launch {
            isLoadingBalance = true
            balanceErrorMessage = null
            planInfoMessage = null
            runCatching { service.balance() }
                .onSuccess {
                    balance = it
                    if (!it.canRequest && it.planName.isBlank()) {
                        planInfoMessage = "No hay plan de vacaciones configurado."
                    }
                }
                .onFailure {
                    balanceErrorMessage = "No se pudieron cargar tus vacaciones."
                }
            hasLoadedBalance = true
            isLoadingBalance = false
        }
    }

    fun loadHistory(force: Boolean = false) {
        if (isLoadingHistory || (hasLoadedHistory && !force)) return
        viewModelScope.launch {
            isLoadingHistory = true
            historyErrorMessage = null
            runCatching { service.currentYearHistory() }
                .onSuccess { historyItems = it.entries }
                .onFailure {
                    historyItems = emptyList()
                    historyErrorMessage = "No se pudo cargar el historial de vacaciones."
                }
            hasLoadedHistory = true
            isLoadingHistory = false
        }
    }

    fun loadPlans(force: Boolean = false) {
        if (hasLoadedPlans && !force) return
        viewModelScope.launch {
            runCatching { service.plans().plans }
                .onSuccess {
                    plans = visibleRequestPlans(it)
                    selectedPlanId = selectedPlanId.ifBlank { plans.firstOrNull()?.id.orEmpty() }
                    hasLoadedPlans = true
                }
                .onFailure {
                    plans = emptyList()
                    hasLoadedPlans = true
                }
        }
    }

    fun openVacationRequest() {
        showVacationRequest = true
        requestErrorMessage = null
        loadRequestData()
    }

    fun loadRequestData() {
        if (isLoadingRequestData || (hasLoadedPlans && hasLoadedTypes)) return
        viewModelScope.launch {
            isLoadingRequestData = true
            requestErrorMessage = null
            runCatching {
                val loadedPlans = if (hasLoadedPlans) plans else service.plans().plans
                val loadedTypes = if (hasLoadedTypes) requestTypes else service.types().types
                loadedPlans to loadedTypes
            }.onSuccess { (loadedPlans, loadedTypes) ->
                plans = visibleRequestPlans(loadedPlans)
                requestTypes = loadedTypes
                selectedPlanId = selectedPlanId.ifBlank { plans.firstOrNull()?.id.orEmpty() }
                selectedTypeId = selectedTypeId.ifBlank { loadedTypes.firstOrNull()?.id.orEmpty() }
                hasLoadedPlans = true
                hasLoadedTypes = true
            }.onFailure {
                requestErrorMessage = "No se pudieron cargar los datos de la solicitud."
            }
            isLoadingRequestData = false
        }
    }

    fun ensureEndDateAfterStart() {
        if (endDate < startDate) {
            endDate = startDate
        }
    }

    fun submitVacationRequest() {
        if (!canSubmit) return
        viewModelScope.launch {
            isSubmitting = true
            requestErrorMessage = null
            val previousHistoryIds = historyItems.map { it.id }.toSet()
            val previousRemaining = balance.totalRemaining
            val request = VacationCreateRequest(
                typeGuid = selectedTypeId,
                effectiveFrom = startDate,
                effectiveTo = endDate,
                notes = notes.trim(),
                reason = reason.trim(),
                category = selectedPlan?.categoryForRequest
            )
            runCatching { service.create(request) }
                .onSuccess { response ->
                    if (response.inserted) {
                        showRequestCreated()
                    } else {
                        if (confirmRequestCreated(previousHistoryIds, previousRemaining, request)) {
                            showRequestCreated()
                        } else {
                            requestErrorMessage = response.messages.ifBlank { "No se pudo enviar la solicitud. Revisa los datos e inténtalo de nuevo." }
                        }
                    }
                }
                .onFailure { error ->
                    if (confirmRequestCreated(previousHistoryIds, previousRemaining, request)) {
                        showRequestCreated()
                    } else {
                        requestErrorMessage = requestErrorMessage(error)
                    }
                }
            isSubmitting = false
        }
    }

    fun requestVacationCancellation(entry: VacationHistoryEntry) {
        vacationEntryToCancel = entry
    }

    fun dismissVacationCancellation() {
        vacationEntryToCancel = null
    }

    fun cancelVacation(entry: VacationHistoryEntry) {
        if (!entry.canRequestCancellation || cancellingEventGuid != null) return
        viewModelScope.launch {
            cancellingEventGuid = entry.eventGuid
            cancellationErrorMessage = null
            cancellationSuccessMessage = null
            runCatching { service.cancel(entry.eventGuid) }
                .onSuccess { response ->
                    cancellationSuccessMessage = response.messages.trim()
                        .ifBlank { "Solicitud de cancelación enviada." }
                    updateHistoryStatus(entry.eventGuid, response.status.ifBlank { "cancellation_pending_approval" })
                    showVacationCancellationSuccess()
                    loadBalance(force = true)
                    loadHistory(force = true)
                }
                .onFailure { error ->
                    cancellationErrorMessage = cancellationErrorMessage(error)
                    showVacationCancellationError()
                }
            vacationEntryToCancel = null
            cancellingEventGuid = null
        }
    }

    fun rescheduleClockNotifications(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            var skipNoRecordToday = false
            val longOpenAt = if (notificationEnabled["longOpenEntry"] == true) {
                runCatching {
                    val status = clockService.status()
                    val entry = status.lastEntry?.toClockEntry()
                    if (status.isClockedIn) entry?.clockIn?.time?.plus(9 * 60 * 60 * 1000L) else null
                }.getOrNull()
            } else {
                null
            }
            if (notificationEnabled["noRecordEndOfDay"] == true) {
                skipNoRecordToday = runCatching {
                    val today = todayString()
                    clockService.history(today, today).entries.isNotEmpty()
                }.getOrDefault(false)
            }
            ClockNotificationScheduler.reschedule(appContext, this@MoreViewModel, longOpenAt, skipNoRecordToday)
        }
    }

    private fun resetRequestFields() {
        reason = ""
        notes = ""
        startDate = todayString()
        endDate = todayString()
    }

    private fun showRequestCreated() {
        showVacationRequest = false
        confirmationTitle = "Solicitud creada"
        confirmationMessage = "Solicitud enviada correctamente."
        showConfirmation = true
        resetRequestFields()
        refresh()
    }

    private fun showVacationCancellationSuccess() {
        confirmationTitle = "Cancelación solicitada"
        confirmationMessage = cancellationSuccessMessage ?: "Solicitud de cancelación enviada."
        showConfirmation = true
    }

    private fun showVacationCancellationError() {
        confirmationTitle = "No se pudo cancelar"
        confirmationMessage = cancellationErrorMessage ?: "No se pudo solicitar la cancelación. Inténtalo de nuevo."
        showConfirmation = true
    }

    private fun updateHistoryStatus(eventGuid: String, status: String) {
        historyItems = historyItems.map { entry ->
            if (entry.eventGuid == eventGuid) entry.copy(status = status) else entry
        }
    }

    private suspend fun confirmRequestCreated(
        previousHistoryIds: Set<String>,
        previousRemaining: Double,
        request: VacationCreateRequest
    ): Boolean {
        val updatedHistory = runCatching { service.currentYearHistory() }.getOrNull()
        if (updatedHistory != null) {
            historyItems = updatedHistory.entries
            hasLoadedHistory = true
            historyErrorMessage = null
            val newMatchingEntry = updatedHistory.entries.any { entry ->
                entry.id !in previousHistoryIds &&
                    entry.effectiveFrom == request.effectiveFrom &&
                    entry.effectiveTo == request.effectiveTo
            }
            if (newMatchingEntry) return true
        }

        val updatedBalance = runCatching { service.balance() }.getOrNull()
        if (updatedBalance != null) {
            balance = updatedBalance
            hasLoadedBalance = true
            balanceErrorMessage = null
            if (updatedBalance.totalRemaining != previousRemaining) return true
        }
        return false
    }

    private fun requestErrorMessage(error: Throwable): String {
        if (error is APIError.Http && error.statusCode == 504) {
            showVacationRequest = false
            confirmationTitle = "Solicitud en proceso"
            confirmationMessage = "Cezanne ha tardado demasiado en responder. Es posible que la solicitud se haya creado correctamente, así que revisa Cezanne antes de volver a enviarla."
            showConfirmation = true
            return ""
        }
        if (error is APIError.Http) {
            extractDetail(error.serverMessage)?.let { return it }
        }
        return "No se pudo enviar la solicitud. Revisa los datos e inténtalo de nuevo."
    }

    private fun cancellationErrorMessage(error: Throwable): String {
        if (error is APIError.Http) {
            extractDetail(error.serverMessage)?.let { detail ->
                if (detail.isApprovedAbsenceWorkflowError()) {
                    return "Solo se pueden cancelar mediante aprobación los eventos de ausencia aprobados."
                }
                return detail
            }
            if (error.statusCode == 404) return "Este evento ya no está disponible."
            if (error.statusCode == 422) return "Cezanne ha rechazado la cancelación de este evento."
        }
        return "No se pudo solicitar la cancelación. Inténtalo de nuevo."
    }

    private fun String.isApprovedAbsenceWorkflowError(): Boolean {
        val normalized = lowercase(Locale.getDefault())
        return normalized.contains("only approved absence") &&
            normalized.contains("deleted using workflow")
    }

    private fun extractDetail(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return null
        val parsed = runCatching {
            val json = JSONObject(trimmed)
            when (val detail = json.opt("detail")) {
                is String -> detail
                is JSONArray -> (0 until detail.length()).mapNotNull { index ->
                    when (val item = detail.opt(index)) {
                        is JSONObject -> item.optString("msg").takeIf { it.isNotBlank() }
                        is String -> item.takeIf { it.isNotBlank() }
                        else -> null
                    }
                }.joinToString("\n")
                else -> null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return parsed ?: trimmed.takeIf { it.length < 240 }
    }

    private fun visibleRequestPlans(source: List<VacationAbsencePlan>): List<VacationAbsencePlan> {
        val wanted = listOf(
            listOf("vacacion", "vacaciones", "holiday", "holidays", "conge", "conges")
            // Para volver a mostrar estos planes en el futuro, añádelos de nuevo a esta lista:
            // listOf("permiso", "permission", "paid leave"),
            // listOf("enfermedad", "sick", "sickness", "illness", "maladie")
        )
        val normalizedPlans = source.map { plan ->
            plan to "${plan.displayName} ${plan.category}".normalizeForSearch()
        }
        return wanted.mapNotNull { needles ->
            normalizedPlans.firstOrNull { (_, haystack) -> needles.any { haystack.contains(it.normalizeForSearch()) } }?.first
        }.distinctBy { it.id }
    }

    private fun String.normalizeForSearch(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private companion object {
        fun todayString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        }
    }
}
