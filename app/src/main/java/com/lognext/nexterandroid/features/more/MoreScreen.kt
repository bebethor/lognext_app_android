package com.lognext.nexterandroid.features.more

import android.Manifest
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
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import java.util.Locale

@Composable
fun MoreScreen(viewModel: MoreViewModel = viewModel()) {
    val context = LocalContext.current
    var pendingNotificationId by remember { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotificationId?.let { id ->
            viewModel.notificationEnabled[id] = granted
        }
        pendingNotificationId = null
    }
    fun setNotificationEnabled(id: String, enabled: Boolean) {
        if (!enabled) {
            viewModel.notificationEnabled[id] = false
            return
        }
        if (hasNotificationPermission(context)) {
            viewModel.notificationEnabled[id] = true
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
        VacationHistoryCard(viewModel.historyItems)
        ClockNotificationsCard(
            viewModel = viewModel,
            onNotificationEnabledChange = { id, enabled -> setNotificationEnabled(id, enabled) },
            onTimeClick = { settingId, currentMinutes ->
                showTimePicker(context, currentMinutes) { selectedMinutes ->
                    viewModel.notificationMinutes[settingId] = selectedMinutes
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
}

@Composable
private fun VacationCard(viewModel: MoreViewModel) {
    SectionCard(title = "🏖️ Vacaciones") {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VacationBox(
                number = formattedVacationNumber(viewModel.balance.totalEntitlement),
                label = "Total año",
                background = subtleBackground(),
                numberColor = NexterColors.primaryText(),
                modifier = Modifier.weight(1f)
            )
            VacationBox(
                number = formattedVacationNumber(viewModel.balance.totalTaken),
                label = "Usados",
                background = NexterColors.Red.copy(alpha = 0.08f),
                numberColor = NexterColors.Red,
                modifier = Modifier.weight(1f)
            )
            VacationBox(
                number = formattedVacationNumber(viewModel.balance.totalRemaining),
                label = "Disponibles",
                background = NexterColors.Green.copy(alpha = 0.12f),
                numberColor = Color(0xFF2A8B3A),
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${viewModel.balance.planName} · ${viewModel.balance.timeUnit} · ${currentYearVacationPeriod()}",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Recuerda que para solicitar vacaciones, has de hacerlo con 15 días de antelación.",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote
            )
        }

        Button(
            onClick = { viewModel.showVacationRequest = true },
            colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.primaryText(), contentColor = requestButtonTextColor()),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 14.dp)
                .height(46.dp)
        ) {
            Text("+ Solicitar vacaciones", fontSize = NexterTypography.Button, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VacationBox(number: String, label: String, background: Color, numberColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(number, color = numberColor, fontSize = NexterTypography.Metric, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = NexterColors.secondaryText(),
            fontSize = NexterTypography.Footnote,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VacationHistoryCard(items: List<VacationHistoryEntry>) {
    SectionCard(title = "Historial de vacaciones") {
        if (items.isEmpty()) {
            Text(
                "No hay solicitudes de vacaciones.",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        } else {
            items.forEachIndexed { index, entry ->
                VacationHistoryRow(entry)
                if (index != items.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 14.dp))
            }
        }
    }
}

@Composable
private fun VacationHistoryRow(entry: VacationHistoryEntry) {
    val statusColor = vacationStatusColor(entry.status)
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.typeName,
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = entry.localizedStatus,
                color = statusColor,
                fontSize = NexterTypography.Badge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Row {
            Text(
                entry.formattedPeriod,
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${formattedVacationNumber(entry.totalDays)} días",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium
            )
        }
        if (entry.approver.isNotBlank()) {
            Text(
                "Aprobador: ${entry.approver}",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote
            )
        }
    }
}

@Composable
private fun ClockNotificationsCard(
    viewModel: MoreViewModel,
    onNotificationEnabledChange: (String, Boolean) -> Unit,
    onTimeClick: (String, Int) -> Unit
) {
    SectionCard(title = "Notificaciones de jornada") {
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
                    setting.title,
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.Body,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    setting.subtitle,
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
                    "Hora",
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
                        "Solicitar vacaciones",
                        color = NexterColors.primaryText(),
                        fontSize = NexterTypography.ScreenTitle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Cancelar",
                        color = NexterColors.Red,
                        fontSize = NexterTypography.SmallButton,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexterColors.Red.copy(alpha = 0.10f))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                    RequestSectionTitle("Plan de vacaciones")
                    RequestInfoRow("Plan", viewModel.balance.planName)
                    if (!viewModel.balance.hideRemainingOnRequest) {
                        RequestInfoRow("Disponible", formattedVacationNumber(viewModel.balance.totalRemaining))
                    }
                    if (!viewModel.balance.allowOverbooking) {
                        Text(
                            "Este plan no permite superar el saldo disponible.",
                            color = NexterColors.secondaryText(),
                            fontSize = NexterTypography.Footnote
                        )
                    }

                    RequestSectionTitle("Fechas")
                    OutlinedTextField(
                        value = viewModel.startDate,
                        onValueChange = { viewModel.startDate = it },
                        label = { Text("Fecha inicio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.endDate,
                        onValueChange = { viewModel.endDate = it },
                        label = { Text("Fecha fin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    RequestSectionTitle("Tipo")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.requestTypes.forEach { type ->
                            val selected = viewModel.selectedTypeId == type.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) NexterColors.Red.copy(alpha = 0.10f) else subtleBackground())
                                    .border(
                                        BorderStroke(1.dp, if (selected) NexterColors.Red.copy(alpha = 0.65f) else NexterColors.border()),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectedTypeId = type.id }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(type.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    type.displayName,
                                    color = NexterColors.primaryText(),
                                    fontSize = NexterTypography.Body,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (type.timeUnits.isNotBlank()) {
                                    Text("(${type.timeUnits})", color = NexterColors.secondaryText(), fontSize = NexterTypography.Footnote)
                                }
                            }
                        }
                    }

                    RequestSectionTitle(if (viewModel.balance.requiresReason) "Motivo *" else "Motivo")
                    OutlinedTextField(
                        value = viewModel.reason,
                        onValueChange = { if (it.length <= 200) viewModel.reason = it },
                        label = { Text("Motivo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                    )
                    Text("${viewModel.reason.length}/200", color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)

                    RequestSectionTitle(if (viewModel.balance.requiresNotes) "Notas *" else "Notas")
                    OutlinedTextField(
                        value = viewModel.notes,
                        onValueChange = { if (it.length <= 500) viewModel.notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                    )
                    Text("${viewModel.notes.length}/500", color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }

                Divider(color = NexterColors.border())
                Button(
                    enabled = viewModel.canSubmit,
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.Red, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .height(46.dp)
                ) {
                    Text("Enviar", fontSize = NexterTypography.Button, fontWeight = FontWeight.SemiBold)
                }
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
                    title,
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
                    message,
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                )
                Divider(color = NexterColors.border())
                Text(
                    "Aceptar",
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
        "rejected", "rechazado", "cancelled", "canceled", "cancelado" -> NexterColors.Red
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

private fun minutesToTime(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hour, minute)
}
