package com.lognext.nexterandroid.features.more

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.BuildConfig
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MoreScreen() {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MoreViewModel(AppDependencies.vacationService, AppDependencies.clockService) as T
        }
    }
    val viewModel: MoreViewModel = viewModel(factory = factory)
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        ClockNotificationScheduler.loadSettings(context, viewModel)
        viewModel.loadIfNeeded()
    }

    var pendingNotificationId by remember { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotificationId?.let { id ->
            viewModel.notificationEnabled[id] = granted
            if (granted) viewModel.rescheduleClockNotifications(context)
        }
        pendingNotificationId = null
    }
    fun setNotificationEnabled(id: String, enabled: Boolean) {
        if (!enabled) {
            viewModel.notificationEnabled[id] = false
            viewModel.rescheduleClockNotifications(context)
            return
        }
        if (hasNotificationPermission(context)) {
            viewModel.notificationEnabled[id] = true
            viewModel.rescheduleClockNotifications(context)
        } else {
            pendingNotificationId = id
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VacationCard(viewModel)
        VacationHistoryCard(viewModel)
        ClockNotificationsCard(
            viewModel = viewModel,
            onNotificationEnabledChange = { id, enabled -> setNotificationEnabled(id, enabled) },
            onTimeClick = { settingId, currentMinutes ->
                showTimePicker(context, currentMinutes) { selectedMinutes ->
                    viewModel.notificationMinutes[settingId] = selectedMinutes
                    viewModel.rescheduleClockNotifications(context)
                }
            }
        )
    }

    if (viewModel.showVacationRequest) {
        VacationRequestDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showVacationRequest = false },
            onSubmit = viewModel::submitVacationRequest
        )
    }

    if (viewModel.showConfirmation) {
        VacationConfirmationDialog(
            title = viewModel.confirmationTitle,
            message = viewModel.confirmationMessage,
            onDismiss = { viewModel.showConfirmation = false }
        )
    }
    viewModel.vacationEntryToCancel?.let { entry ->
        VacationCancellationAlert(
            onDismiss = viewModel::dismissVacationCancellation,
            onConfirm = { viewModel.cancelVacation(entry) }
        )
    }
}

@Composable
private fun VacationCard(viewModel: MoreViewModel) {
    SectionCard(title = stringResource(R.string.more_vacations)) {
        if (viewModel.isLoadingBalance || !viewModel.hasLoadedBalance) {
            LoadingBlock(stringResource(R.string.more_loading_absence_events))
            return@SectionCard
        }
        viewModel.balanceErrorMessage?.let { message ->
            ErrorBlock(message = localizedMoreMessage(message), onRetry = { viewModel.loadBalance(force = true) })
            return@SectionCard
        }

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PaidVacationSummaryCard(viewModel)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactAbsenceSummaryCard(
                    title = stringResource(R.string.more_permissions_fallback),
                    usedValue = "0",
                    usedUnit = "",
                    episodes = "0",
                    modifier = Modifier.weight(1f)
                )
                CompactAbsenceSummaryCard(
                    title = stringResource(R.string.more_sickness_fallback),
                    usedValue = "0",
                    usedUnit = "",
                    episodes = "0",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${vacationPlanName(viewModel.balance.planName)} · ${vacationTimeUnit(viewModel.balance.timeUnit)} · ${currentYearVacationPeriod()}",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.more_vacation_notice),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote
            )
        }

        if (viewModel.balance.canRequest) {
            Button(
                onClick = { viewModel.openVacationRequest() },
                colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.primaryText(), contentColor = requestButtonTextColor()),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .height(46.dp)
            ) {
                Text(stringResource(R.string.more_request_vacation), fontSize = NexterTypography.Button, fontWeight = FontWeight.SemiBold)
            }
        } else if (viewModel.planInfoMessage != null) {
            Text(
                text = localizedMoreMessage(viewModel.planInfoMessage.orEmpty()),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun PaidVacationSummaryCard(viewModel: MoreViewModel) {
    val remainingRatio = vacationRemainingRatio(viewModel.balance)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(absenceSummaryBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = vacationPlanName(viewModel.balance.planName),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.more_annual_right, formattedVacationNumber(viewModel.balance.totalEntitlement)),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(vacationProgressTrackColor())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(remainingRatio)
                    .height(10.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF188052))
            )
        }

        Row(verticalAlignment = Alignment.Top) {
            MetricStack(
                title = stringResource(R.string.more_used_single),
                value = formattedVacationNumber(viewModel.balance.totalTaken),
                unit = stringResource(R.string.more_days_unit_cap),
                alignEnd = false,
                valueColor = NexterColors.primaryText(),
                modifier = Modifier.weight(1f)
            )
            MetricStack(
                title = stringResource(R.string.more_remaining),
                value = formattedVacationNumber(viewModel.balance.totalRemaining),
                unit = stringResource(R.string.more_days_unit_cap),
                alignEnd = true,
                valueColor = Color(0xFF188052),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactAbsenceSummaryCard(
    title: String,
    usedValue: String,
    usedUnit: String,
    episodes: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(absenceSummaryBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = NexterColors.primaryText(),
            fontSize = NexterTypography.Callout,
            fontWeight = FontWeight.SemiBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.Top) {
            MetricStack(
                title = stringResource(R.string.more_used_single),
                value = usedValue,
                unit = usedUnit,
                alignEnd = false,
                valueColor = NexterColors.primaryText(),
                modifier = Modifier.weight(1f)
            )
            MetricStack(
                title = stringResource(R.string.more_episodes),
                value = episodes,
                unit = "",
                alignEnd = false,
                valueColor = NexterColors.primaryText(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricStack(
    title: String,
    value: String,
    unit: String,
    alignEnd: Boolean,
    valueColor: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption)
        Text(value, color = valueColor, fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
        if (unit.isNotEmpty()) {
            Text(
                unit,
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VacationHistoryCard(viewModel: MoreViewModel) {
    SectionCard(title = stringResource(R.string.more_absence_history)) {
        if (viewModel.isLoadingHistory || !viewModel.hasLoadedHistory) {
            LoadingBlock(stringResource(R.string.more_loading_history))
            return@SectionCard
        }
        viewModel.historyErrorMessage?.let { message ->
            ErrorBlock(message = localizedMoreMessage(message), onRetry = { viewModel.loadHistory(force = true) })
            return@SectionCard
        }
        val items = vacationHistoryItems(viewModel)
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.more_no_vacation_requests),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        } else {
            items.forEachIndexed { index, entry ->
                VacationHistoryRow(entry, viewModel)
                if (index != items.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 14.dp))
            }
        }
    }
}

private fun vacationHistoryItems(viewModel: MoreViewModel): List<VacationHistoryEntry> {
    if (!BuildConfig.DEBUG) return viewModel.historyItems
    val mockEntry = VacationHistoryEntry(
        eventGuid = "mock-approved-vacation-event",
        typeName = "Vacaciones retribuidas",
        planName = "Vacaciones retribuidas",
        status = if (viewModel.mockVacationCancellationRequested) "cancellation_pending_approval" else "approved",
        effectiveFrom = "2026-08-05",
        effectiveTo = "2026-08-07",
        totalDays = 3.0,
        approver = "Responsable directo",
        notes = "Fila temporal para revisar diseño",
        reason = "Mock"
    )
    return listOf(mockEntry) + viewModel.historyItems
}

@Composable
private fun VacationHistoryRow(entry: VacationHistoryEntry, viewModel: MoreViewModel) {
    val statusColor = vacationStatusColor(entry.status)
    val isCancelling = viewModel.cancellingEventGuid == entry.eventGuid
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = vacationHistoryTitle(entry),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (!entry.isCancellationPending) {
                Text(
                    text = vacationStatusText(entry.status),
                    color = statusColor,
                    fontSize = NexterTypography.Badge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Row {
            Text(
                vacationHistoryPeriod(entry),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.more_days, formattedVacationNumber(entry.totalDays)),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium
            )
        }
        if (entry.approver.isNotBlank()) {
            Text(
                stringResource(R.string.more_approver, entry.approver),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote
            )
        }
        if (entry.isCancellationPending) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NexterColors.Blue.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("◷", color = NexterColors.Blue, fontSize = NexterTypography.Footnote, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.vacation_cancellation_pending),
                    color = NexterColors.Blue,
                    fontSize = NexterTypography.Footnote,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (entry.canRequestCancellation) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isCancelling) { viewModel.requestVacationCancellation(entry) }
                    .background(NexterColors.Red.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Text("⊗", color = NexterColors.Red, fontSize = NexterTypography.Footnote, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    stringResource(if (isCancelling) R.string.vacation_requesting_cancellation else R.string.vacation_request_cancellation),
                    color = NexterColors.Red,
                    fontSize = NexterTypography.Footnote,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VacationCancellationAlert(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = NexterColors.cardBackground(),
        title = {
            Text(
                stringResource(R.string.vacation_cancel_event_title),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.CardTitle,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.vacation_cancel_event_message),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Body
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.vacation_request_cancellation),
                    color = NexterColors.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.vacation_do_not_cancel),
                    color = NexterColors.secondaryText(),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun ClockNotificationsCard(
    viewModel: MoreViewModel,
    onNotificationEnabledChange: (String, Boolean) -> Unit,
    onTimeClick: (String, Int) -> Unit
) {
    SectionCard(title = stringResource(R.string.more_clock_notifications)) {
        viewModel.notificationSettings.forEachIndexed { index, setting ->
            val currentMinutes = viewModel.notificationMinutes[setting.id] ?: setting.defaultMinutes
            ClockNotificationRow(
                setting = setting,
                enabled = viewModel.notificationEnabled[setting.id] == true,
                minutes = currentMinutes,
                onEnabledChange = { onNotificationEnabledChange(setting.id, it) },
                onTimeClick = { onTimeClick(setting.id, currentMinutes) }
            )
            if (index != viewModel.notificationSettings.lastIndex) Divider(color = NexterColors.border())
        }
    }
}

@Composable
private fun ClockNotificationRow(
    setting: ClockNotificationSetting,
    enabled: Boolean,
    minutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    notificationTitle(setting.id),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.Body,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    notificationSubtitle(setting.id),
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.Footnote
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(checkedThumbColor = NexterColors.Red, checkedTrackColor = NexterColors.Red.copy(alpha = 0.35f))
            )
        }
        if (setting.hasTime) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(subtleBackground())
                    .clickable(enabled = enabled, onClick = onTimeClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.more_time),
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.Callout,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    minutesToTime(minutes),
                    color = if (enabled) NexterColors.primaryText() else NexterColors.tertiaryText(),
                    fontSize = NexterTypography.Callout,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VacationRequestDialog(viewModel: MoreViewModel, onDismiss: () -> Unit, onSubmit: () -> Unit) {
    var showTypePicker by remember { mutableStateOf(false) }
    val selectedPlan = viewModel.selectedPlan
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            backgroundColor = NexterColors.cardBackground(),
            shape = RoundedCornerShape(18.dp),
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = NexterColors.Red,
                        fontSize = NexterTypography.SmallButton,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .width(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(top = 8.dp, bottom = 8.dp)
                    )
                    Text(
                        stringResource(R.string.more_request_vacation_title),
                        color = NexterColors.primaryText(),
                        fontSize = NexterTypography.SectionTitle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.more_send),
                        color = if (viewModel.canSubmit) NexterColors.Red else NexterColors.tertiaryText(),
                        fontSize = NexterTypography.SmallButton,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .width(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = viewModel.canSubmit, onClick = onSubmit)
                            .padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                Divider(color = NexterColors.border())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (viewModel.isLoadingRequestData) {
                        LoadingBlock(stringResource(R.string.more_loading_types))
                    } else {
                        viewModel.requestErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                            ErrorBlock(message = localizedMoreMessage(message), onRetry = viewModel::loadRequestData)
                        }

                        VacationFormSection(title = stringResource(R.string.more_dates)) {
                            VacationDateSelectorRow(
                                label = stringResource(R.string.more_start_date),
                                value = formatRequestDateDisplay(viewModel.startDate),
                                onClick = {
                                    showVacationDatePicker(context, viewModel.startDate) { selected ->
                                        viewModel.startDate = selected
                                        viewModel.ensureEndDateAfterStart()
                                    }
                                }
                            )
                            Divider(color = NexterColors.border())
                            VacationDateSelectorRow(
                                label = stringResource(R.string.more_end_date),
                                value = formatRequestDateDisplay(viewModel.endDate),
                                onClick = {
                                    showVacationDatePicker(context, viewModel.endDate) { selected ->
                                        viewModel.endDate = selected
                                    }
                                }
                            )
                        }

                        VacationFormSection(title = stringResource(R.string.more_type)) {
                            if (selectedPlan == null) {
                                Text(
                                    text = stringResource(R.string.more_no_absence_types),
                                    color = NexterColors.secondaryText(),
                                    fontSize = NexterTypography.Callout,
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                VacationPlanSelectorRow(
                                    selectedPlan = selectedPlan,
                                    onClick = { showTypePicker = true },
                                    showContainer = false
                                )
                            }
                        }

                        VacationFormSection(title = if (viewModel.balance.requiresReason) stringResource(R.string.more_reason_required) else stringResource(R.string.more_reason)) {
                            OutlinedTextField(
                                value = viewModel.reason,
                                onValueChange = { if (it.length <= 200) viewModel.reason = it },
                                label = { Text(stringResource(R.string.more_reason)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Text("${viewModel.reason.length}/200", color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption, modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 10.dp), textAlign = TextAlign.End)
                        }

                        VacationFormSection(title = if (viewModel.balance.requiresNotes) stringResource(R.string.more_notes_required) else stringResource(R.string.more_notes)) {
                            OutlinedTextField(
                                value = viewModel.notes,
                                onValueChange = { if (it.length <= 500) viewModel.notes = it },
                                label = { Text(stringResource(R.string.more_notes)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(136.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Text("${viewModel.notes.length}/500", color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption, modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 10.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }

        if (viewModel.isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexterColors.cardBackground())
                        .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(12.dp))
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = NexterColors.Red)
                    Text(
                        text = stringResource(R.string.more_sending_request),
                        color = NexterColors.primaryText(),
                        fontSize = NexterTypography.Button,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showTypePicker && viewModel.plans.isNotEmpty()) {
        VacationPlanPickerDialog(
            plans = viewModel.plans,
            selectedPlanId = viewModel.selectedPlanId,
            onSelected = { plan ->
                viewModel.selectPlan(plan)
                showTypePicker = false
            },
            onDismiss = { showTypePicker = false }
        )
    }
}

@Composable
private fun VacationPlanSelectorRow(
    selectedPlan: VacationAbsencePlan,
    onClick: () -> Unit,
    showContainer: Boolean = true
) {
    val rowModifier = if (showContainer) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(subtleBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(12.dp))
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(vacationPlanColor(selectedPlan))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.more_absence_type),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium
            )
            Text(
                vacationPlanDisplayName(selectedPlan),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text("›", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VacationFormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = NexterColors.primaryText(),
            fontSize = NexterTypography.Body,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NexterColors.cardBackground())
                .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(12.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun VacationDateSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = NexterColors.primaryText(),
            fontSize = NexterTypography.Body,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VacationPlanPickerDialog(
    plans: List<VacationAbsencePlan>,
    selectedPlanId: String,
    onSelected: (VacationAbsencePlan) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = NexterColors.cardBackground(),
            shape = RoundedCornerShape(14.dp),
            elevation = 18.dp,
            border = BorderStroke(1.dp, NexterColors.border()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    stringResource(R.string.more_vacation_type),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.CardTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                )
                Divider(color = NexterColors.border())
                plans.forEachIndexed { index, plan ->
                    val selected = plan.id == selectedPlanId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(plan) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelected(plan) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NexterColors.Red,
                                unselectedColor = NexterColors.tertiaryText()
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(vacationPlanColor(plan))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            vacationPlanDisplayName(plan),
                            color = NexterColors.primaryText(),
                            fontSize = NexterTypography.Body,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index != plans.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 58.dp))
                }
                Divider(color = NexterColors.border())
                Text(
                    stringResource(R.string.cancel),
                    color = NexterColors.Red,
                    fontSize = NexterTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun VacationConfirmationDialog(title: String, message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = NexterColors.cardBackground(),
            shape = RoundedCornerShape(14.dp),
            elevation = 18.dp,
            border = BorderStroke(1.dp, NexterColors.border()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    vacationConfirmationTitle(title),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.CardTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                )
                Divider(color = NexterColors.border())
                Text(
                    vacationConfirmationMessage(message),
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                )
                Divider(color = NexterColors.border())
                Text(
                    stringResource(R.string.accept),
                    color = NexterColors.Red,
                    fontSize = NexterTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun RequestSectionTitle(text: String) {
    Text(text, color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RequestInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(subtleBackground())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, modifier = Modifier.weight(1f))
        Text(value, color = NexterColors.primaryText(), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, NexterColors.border()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                title,
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.CardTitle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
            Divider(color = NexterColors.border())
            content()
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = message,
            color = NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.retry),
            color = NexterColors.Red,
            fontSize = NexterTypography.SmallButton,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun vacationTypeLabel(typeId: String): String {
    return when (typeId) {
        "type-holiday" -> stringResource(R.string.vacation_type_holiday)
        "type-non-working" -> stringResource(R.string.vacation_type_non_working)
        "type-festivity" -> stringResource(R.string.vacation_type_festivity)
        "type-vacation" -> stringResource(R.string.vacation_type_vacation)
        else -> typeId
    }
}

@Composable
private fun vacationTypeDisplayName(type: VacationRequestType): String {
    return when (type.id) {
        "type-holiday", "type-non-working", "type-festivity", "type-vacation" -> vacationTypeLabel(type.id)
        else -> type.displayName.ifBlank { type.name }.ifBlank { type.id }
    }
}

@Composable
private fun vacationPlanDisplayName(plan: VacationAbsencePlan): String {
    val normalized = "${plan.displayName} ${plan.category}".lowercase(Locale.getDefault())
    return when {
        normalized.contains("vacacion") || normalized.contains("holiday") || normalized.contains("cong") -> stringResource(R.string.more_paid_leave_fallback)
        normalized.contains("permiso") || normalized.contains("permission") || normalized.contains("paid leave") -> stringResource(R.string.more_permissions_fallback)
        normalized.contains("enfermedad") || normalized.contains("sick") || normalized.contains("illness") || normalized.contains("maladie") -> stringResource(R.string.more_sickness_fallback)
        else -> plan.displayName
    }
}

private fun vacationPlanColor(plan: VacationAbsencePlan): Color {
    val normalized = "${plan.displayName} ${plan.category}".lowercase(Locale.getDefault())
    return when {
        normalized.contains("vacacion") || normalized.contains("holiday") || normalized.contains("cong") -> NexterColors.Red
        normalized.contains("permiso") || normalized.contains("permission") || normalized.contains("paid leave") -> NexterColors.Blue
        normalized.contains("enfermedad") || normalized.contains("sick") || normalized.contains("illness") || normalized.contains("maladie") -> NexterColors.Violet
        else -> NexterColors.Red
    }
}

@Composable
private fun vacationHistoryType(typeName: String): String {
    return when (typeName.lowercase(Locale.getDefault())) {
        "festivo" -> stringResource(R.string.vacation_type_holiday)
        "no laborable" -> stringResource(R.string.vacation_type_non_working)
        "festividad" -> stringResource(R.string.vacation_type_festivity)
        "vacaciones" -> stringResource(R.string.vacation_type_vacation)
        else -> typeName
    }
}

@Composable
private fun vacationHistoryTitle(entry: VacationHistoryEntry): String {
    val typeName = entry.typeName.trim()
    if (typeName.isNotEmpty()) return vacationHistoryType(typeName)
    val planName = entry.planName.trim()
    if (planName.isNotEmpty()) return vacationPlanName(planName)
    return stringResource(R.string.more_request_vacation_title)
}

@Composable
private fun vacationHistoryPeriod(entry: VacationHistoryEntry): String {
    return formatVacationPeriod(entry.effectiveFrom, entry.effectiveTo) ?: stringResource(R.string.more_dates_unavailable)
}

@Composable
private fun vacationPlanName(planName: String): String {
    return when (planName.lowercase(Locale.getDefault())) {
        "vacaciones anuales" -> stringResource(R.string.vacation_annual_plan)
        else -> planName
    }
}

@Composable
private fun vacationTimeUnit(unit: String): String {
    return when (unit.lowercase(Locale.getDefault())) {
        "días", "dias", "days", "jours" -> stringResource(R.string.vacation_days_unit)
        else -> unit
    }
}

@Composable
private fun vacationConfirmationTitle(title: String): String {
    return when (title.lowercase(Locale.getDefault())) {
        "solicitud creada" -> stringResource(R.string.vacation_request_created)
        "solicitud en proceso" -> stringResource(R.string.vacation_request_pending_title)
        "cancelación solicitada" -> stringResource(R.string.vacation_cancellation_requested_title)
        "no se pudo cancelar" -> stringResource(R.string.vacation_cancellation_failed_title)
        else -> title
    }
}

@Composable
private fun vacationConfirmationMessage(message: String): String {
    return when (message.lowercase(Locale.getDefault())) {
        "solicitud enviada correctamente." -> stringResource(R.string.vacation_request_sent)
        "cezanne ha tardado demasiado en responder. es posible que la solicitud se haya creado correctamente, así que revisa cezanne antes de volver a enviarla." -> stringResource(R.string.vacation_request_timeout_message)
        "solicitud de cancelación enviada." -> stringResource(R.string.vacation_cancellation_sent)
        "no se pudo solicitar la cancelación. inténtalo de nuevo." -> stringResource(R.string.vacation_cancellation_error)
        "este evento ya no está disponible." -> stringResource(R.string.vacation_cancellation_not_available)
        "cezanne ha rechazado la cancelación de este evento." -> stringResource(R.string.vacation_cancellation_rejected)
        else -> message
    }
}

@Composable
private fun localizedMoreMessage(message: String): String {
    return when (message.lowercase(Locale.getDefault())) {
        "no hay plan de vacaciones configurado." -> stringResource(R.string.more_no_vacation_plan_configured)
        "no se pudieron cargar tus vacaciones." -> stringResource(R.string.more_error_load_vacations)
        "no se pudo cargar el historial de vacaciones." -> stringResource(R.string.more_error_load_history)
        "no se pudieron cargar los datos de la solicitud." -> stringResource(R.string.more_error_load_request_data)
        "no se pudo enviar la solicitud. revisa los datos e inténtalo de nuevo." -> stringResource(R.string.more_error_submit_request)
        else -> message
    }
}

@Composable
private fun vacationStatusText(status: String): String {
    return when (status.lowercase()) {
        "approved", "aprobado", "accepted" -> stringResource(R.string.status_approved)
        "rejected", "rechazado" -> stringResource(R.string.status_rejected)
        "cancelled", "canceled", "cancelado" -> stringResource(R.string.status_cancelled)
        "cancelando", "cancellation pending", "cancellation_pending_approval", "cancel pending" -> stringResource(R.string.vacation_cancellation_pending)
        "pending", "submitted", "approval pending", "pending approval", "aprobación pendiente" -> stringResource(R.string.status_pending)
        else -> status
    }
}

@Composable
private fun notificationTitle(id: String): String {
    return when (id) {
        "workStart" -> stringResource(R.string.notification_work_start_title)
        "lunchStart" -> stringResource(R.string.notification_lunch_start_title)
        "lunchEnd" -> stringResource(R.string.notification_lunch_end_title)
        "workEnd" -> stringResource(R.string.notification_work_end_title)
        "longOpenEntry" -> stringResource(R.string.notification_long_open_entry_title)
        "noRecordEndOfDay" -> stringResource(R.string.notification_no_record_title)
        else -> id
    }
}

@Composable
private fun notificationSubtitle(id: String): String {
    return when (id) {
        "workStart" -> stringResource(R.string.notification_work_start_subtitle)
        "lunchStart" -> stringResource(R.string.notification_lunch_start_subtitle)
        "lunchEnd" -> stringResource(R.string.notification_lunch_end_subtitle)
        "workEnd" -> stringResource(R.string.notification_work_end_subtitle)
        "longOpenEntry" -> stringResource(R.string.notification_long_open_entry_subtitle)
        "noRecordEndOfDay" -> stringResource(R.string.notification_no_record_subtitle)
        else -> id
    }
}

@Composable
private fun absenceSummaryBackground(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) {
        subtleBackground()
    } else {
        Color(0xFFF5FBFF)
    }
}

@Composable
private fun vacationProgressTrackColor(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
}

private fun vacationRemainingRatio(balance: MoreVacationBalance): Float {
    if (balance.totalEntitlement <= 0.0) return 0f
    return (balance.totalRemaining / balance.totalEntitlement).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun subtleBackground(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) {
        Color.White.copy(alpha = 0.06f)
    } else {
        NexterColors.Navy.copy(alpha = 0.05f)
    }
}

@Composable
private fun requestButtonTextColor(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) NexterColors.Navy else Color.White
}

private fun vacationStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "approved", "aprobado", "accepted" -> NexterColors.Green
        "rejected", "rechazado", "cancelled", "canceled", "cancelado" -> Color.Red
        "cancelando", "cancellation pending", "cancellation_pending_approval", "cancel pending" -> NexterColors.Blue
        else -> NexterColors.Red
    }
}

private fun formattedVacationNumber(value: Double): String {
    return if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun currentYearVacationPeriod(): String {
    val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    return "01/01/$year a 31/12/$year"
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun showTimePicker(context: Context, minutes: Int, onTimeSelected: (Int) -> Unit) {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour * 60 + selectedMinute)
        },
        hour,
        minute,
        true
    ).show()
}

private fun showVacationDatePicker(context: Context, currentValue: String, onDateSelected: (String) -> Unit) {
    val initial = parseRequestDate(currentValue) ?: Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(formatRequestDate(selected))
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatRequestDateDisplay(value: String): String {
    val calendar = parseRequestDate(value) ?: return value
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        .format(calendar.time)
        .replace(".", "")
}

private fun formatRequestDate(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun parseRequestDate(value: String): Calendar? {
    if (value.isBlank()) return null
    val date = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) }.getOrNull() ?: return null
    return Calendar.getInstance().apply { time = date }
}

private fun minutesToTime(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hour, minute)
}
