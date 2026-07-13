package com.lognext.nexterandroid.features.clock

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import kotlinx.coroutines.delay
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ClockScreen() {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClockViewModel(AppDependencies.clockService) as T
        }
    }
    val viewModel: ClockViewModel = viewModel(factory = factory)

    LaunchedEffect(viewModel) {
        viewModel.loadIfNeeded()
    }

    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.tick(Date())
            delay(1_000)
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
        WorkdayCard(viewModel)
        viewModel.errorMessage?.let { ClockErrorCard(localizedClockMessage(it)) }
        TodayRecordCard(viewModel)
        WeekSummaryCard(viewModel)
        RecentHistoryCard(viewModel)
    }
}

@Composable
private fun ClockErrorCard(message: String) {
    Text(
        text = message,
        color = NexterColors.Red,
        fontSize = NexterTypography.Callout,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexterColors.Red.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, NexterColors.Red.copy(alpha = 0.20f)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
}

@Composable
private fun WorkdayCard(viewModel: ClockViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF04042B))
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = (-40).dp)
                .rotate(-28f)
                .alpha(0.7f)
                .border(BorderStroke(1.dp, Color(0xFF82173A)), RoundedCornerShape(1.dp))
        )

        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.clock_record_title),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = NexterTypography.Callout,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = formattedTime(viewModel.currentTime),
                color = Color.White,
                fontSize = NexterTypography.Clock,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (viewModel.isLoadingStatus && !viewModel.hasLoadedTodayEntries) {
                LoadingInline(stringResource(R.string.clock_loading_workday), light = true)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isCheckedIn) Color.Green else Color.White.copy(alpha = 0.45f))
                    )
                    Text(
                        text = clockStatusText(viewModel),
                        color = if (viewModel.isCheckedIn) Color.Green else Color.White.copy(alpha = 0.55f),
                        fontSize = NexterTypography.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                ClockActionButton(viewModel)
            }
        }
    }
}

@Composable
private fun ClockActionButton(viewModel: ClockViewModel) {
    val isExitAction = if (viewModel.isSubmittingAction) !viewModel.submittingClockIn else viewModel.isCheckedIn
    val accent = if (isExitAction) NexterColors.Red else Color.Green
    val background = if (isExitAction) Color(0xFF37081F) else Color(0xFF083420)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.70f)), RoundedCornerShape(8.dp))
            .clickable(enabled = !viewModel.isSubmittingAction) { viewModel.handleClockButton() }
            .alpha(if (viewModel.isSubmittingAction) 0.65f else 1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (viewModel.isSubmittingAction) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Text(
                text = if (viewModel.isCheckedIn) "■" else "→",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = clockButtonTitle(viewModel),
            color = accent,
            fontSize = NexterTypography.Button,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TodayRecordCard(viewModel: ClockViewModel) {
    SectionCard(title = stringResource(R.string.clock_today_record)) {
        when {
            viewModel.isLoadingStatus || !viewModel.hasLoadedTodayEntries -> {
                LoadingBlock(stringResource(R.string.clock_loading_records))
            }
            todayTimelineRows(viewModel).isEmpty() -> {
                EmptyBlock(
                    title = stringResource(R.string.clock_no_records),
                    subtitle = stringResource(R.string.clock_no_records_subtitle)
                )
            }
            else -> {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    val rows = todayTimelineRows(viewModel)
                    rows.forEachIndexed { index, row ->
                        RecordRow(row)
                        if (index != rows.lastIndex) {
                            Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(row: ClockTimelineRow) {
    val isEntry = row.kind == ClockTimelineKind.Entry
    val color = if (isEntry) Color.Green else NexterColors.Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formattedTime(row.date),
            color = NexterColors.tertiaryText(),
            fontSize = NexterTypography.Footnote,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (isEntry) stringResource(R.string.clock_entry) else stringResource(R.string.clock_exit),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.clock_registered_ok),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout
            )
        }
        Text(
            text = "✓ OK",
            color = color,
            fontSize = NexterTypography.Badge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun WeekSummaryCard(viewModel: ClockViewModel) {
    SectionCard(title = stringResource(R.string.clock_week)) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                shortWeekdayLabels().forEachIndexed { index, label ->
                    WeekDayView(viewModel, index, label, Modifier.weight(1f))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(subtleBackground())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.clock_total_week),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.Callout,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedWeeklyWorkedTime(viewModel),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.Body,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WeekDayView(viewModel: ClockViewModel, dayOffset: Int, label: String, modifier: Modifier) {
    val date = weekDate(dayOffset)
    val seconds = workedSecondsOn(viewModel, date)
    val isToday = isSameDay(date, Date())
    val hasWorkedTime = seconds > 0L
    val value = if (hasWorkedTime) formattedHours(seconds) else "–"
    val text = when {
        isToday -> stringResource(R.string.clock_today)
        hasWorkedTime -> "✓"
        else -> "–"
    }
    val foreground = when {
        isToday -> NexterColors.Blue
        hasWorkedTime -> Color.Green
        else -> Color.Gray
    }
    val background = when {
        isToday -> NexterColors.Blue.copy(alpha = 0.12f)
        hasWorkedTime -> Color.Green.copy(alpha = 0.15f)
        else -> subtleBackground()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout)
        Text(
            text = text,
            color = foreground,
            fontSize = if (isToday) NexterTypography.Caption else NexterTypography.Callout,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(if (isToday) 44.dp else 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .border(
                    BorderStroke(if (isToday) 2.dp else 0.dp, if (isToday) foreground else Color.Transparent),
                    RoundedCornerShape(8.dp)
                )
                .padding(top = if (isToday) 14.dp else 6.dp)
        )
        Text(
            text = value,
            color = if (isToday) foreground else NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentHistoryCard(viewModel: ClockViewModel) {
    SectionCard(title = stringResource(R.string.clock_recent_history)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            HistoryHeaderRow()
            when {
                viewModel.isLoadingStatus || !viewModel.hasLoadedTodayEntries -> {
                    LoadingBlock(stringResource(R.string.clock_loading_history))
                }
                recentHistoryRows(viewModel).isEmpty() -> {
                    Text(
                        text = stringResource(R.string.clock_no_recent_history),
                        color = NexterColors.secondaryText(),
                        fontSize = NexterTypography.Body,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
                else -> {
                    val rows = recentHistoryRows(viewModel)
                    rows.forEachIndexed { index, row ->
                        HistoryRow(row, showDivider = index != rows.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        HistoryHeaderText(stringResource(R.string.clock_date), Modifier.weight(1f))
        HistoryHeaderText(stringResource(R.string.clock_entry), Modifier.weight(1f))
        HistoryHeaderText(stringResource(R.string.clock_exit), Modifier.weight(1f))
        HistoryHeaderText(stringResource(R.string.clock_total), Modifier.weight(1f))
    }
    Divider(color = NexterColors.border())
}

@Composable
private fun HistoryHeaderText(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = NexterColors.tertiaryText(),
        fontSize = NexterTypography.Footnote,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
private fun HistoryRow(row: RecentHistoryRow, showDivider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryText(row.dateText, FontWeight.Normal, Modifier.weight(1f))
        HistoryText(row.entryText, FontWeight.Normal, Modifier.weight(1f))
        HistoryText(row.exitText, FontWeight.Normal, Modifier.weight(1f))
        HistoryText(row.totalText, FontWeight.SemiBold, Modifier.weight(1f))
    }
    if (showDivider) Divider(color = NexterColors.border())
}

@Composable
private fun HistoryText(text: String, weight: FontWeight, modifier: Modifier) {
    Text(
        text = text,
        color = NexterColors.primaryText(),
        fontSize = 13.sp,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.SectionTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Divider(color = NexterColors.border())
            content()
        }
    }
}

@Composable
private fun LoadingInline(text: String, light: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = if (light) Color.White else NexterColors.Red,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (light) Color.White.copy(alpha = 0.65f) else NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(color = NexterColors.Red, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        Text(text = text, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyBlock(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, color = NexterColors.secondaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body, textAlign = TextAlign.Center)
    }
}

@Composable
private fun subtleBackground(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) {
        Color.White.copy(alpha = 0.06f)
    } else {
        NexterColors.Navy.copy(alpha = 0.04f)
    }
}

private fun todayTimelineRows(viewModel: ClockViewModel): List<ClockTimelineRow> {
    return viewModel.todayEntries.flatMap { entry ->
        buildList {
            entry.clockIn?.let { add(ClockTimelineRow(ClockTimelineKind.Entry, it, entry.recordId)) }
            entry.clockOut?.let { add(ClockTimelineRow(ClockTimelineKind.Exit, it, entry.recordId)) }
        }
    }.sortedBy { it.date.time }
}

private fun recentHistoryRows(viewModel: ClockViewModel): List<RecentHistoryRow> {
    val today = ClockViewModel.dayString(Date())
    return viewModel.weekEntries
        .groupBy { ClockViewModel.dayString(it.clockIn ?: it.clockOut ?: Date(0)) }
        .filterKeys { it != today }
        .mapNotNull { (dayKey, entries) ->
            val firstEntry = entries.mapNotNull { it.clockIn }.minByOrNull { it.time }
            val lastExit = entries.mapNotNull { it.clockOut }.maxByOrNull { it.time }
            if (firstEntry == null && lastExit == null) return@mapNotNull null

            val date = firstEntry ?: lastExit ?: return@mapNotNull null
            RecentHistoryRow(
                dayKey = dayKey,
                dateText = formattedHistoryDate(date),
                entryText = firstEntry?.let(::formattedTime) ?: "--:--",
                exitText = lastExit?.let(::formattedTime) ?: "--:--",
                totalText = formattedDuration(entries.sumOf { viewModel.workedSeconds(it) })
            )
        }
        .sortedByDescending { it.dayKey }
        .take(5)
}

@Composable
private fun clockStatusText(viewModel: ClockViewModel): String {
    if (viewModel.isLoadingStatus) return stringResource(R.string.clock_checking_status)
    if (viewModel.isSubmittingAction) {
        return if (viewModel.submittingClockIn) {
            stringResource(R.string.clock_registering_entry)
        } else {
            stringResource(R.string.clock_registering_exit)
        }
    }
    if (viewModel.isCheckedIn && viewModel.clockInTime != null) {
        return stringResource(R.string.clock_checked_in_at, formattedTime(viewModel.clockInTime!!))
    }
    if (viewModel.clockOutTime != null) {
        return stringResource(R.string.clock_out_at, formattedTime(viewModel.clockOutTime!!))
    }
    return stringResource(R.string.clock_out)
}

@Composable
private fun clockButtonTitle(viewModel: ClockViewModel): String {
    if (viewModel.isSubmittingAction) {
        return if (viewModel.submittingClockIn) {
            stringResource(R.string.clock_punching_entry)
        } else {
            stringResource(R.string.clock_punching_exit)
        }
    }
    return if (viewModel.isCheckedIn) stringResource(R.string.clock_punch_exit) else stringResource(R.string.clock_punch_entry)
}

@Composable
private fun localizedClockMessage(message: String): String {
    return when (message.lowercase(Locale.getDefault())) {
        "no se pudieron cargar todos los datos de jornada." -> stringResource(R.string.clock_error_load_all)
        "ya tienes una entrada abierta. primero debes fichar salida." -> stringResource(R.string.clock_error_entry_open)
        "no se pudo registrar la entrada. inténtalo de nuevo en unos minutos." -> stringResource(R.string.clock_error_clock_in)
        "no tienes ninguna entrada abierta. primero debes fichar entrada." -> stringResource(R.string.clock_error_no_open_entry)
        "no se pudo registrar la salida. inténtalo de nuevo en unos minutos." -> stringResource(R.string.clock_error_clock_out)
        else -> message
    }
}

private fun shortWeekdayLabels(): List<String> {
    val weekdays = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
    return listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        .map { weekdays[it].replace(".", "").replaceFirstChar { char -> char.uppercase(Locale.getDefault()) } }
}

private fun formattedWeeklyWorkedTime(viewModel: ClockViewModel): String {
    val totalHours = viewModel.weekEntries.sumOf { viewModel.workedSeconds(it) } / 3600.0
    return String.format(Locale.getDefault(), "%.1fh / 40h", totalHours)
}

private fun workedSecondsOn(viewModel: ClockViewModel, date: Date): Long {
    return viewModel.weekEntries.sumOf { entry ->
        val clockIn = entry.clockIn
        if (clockIn != null && isSameDay(clockIn, date)) viewModel.workedSeconds(entry) else 0L
    }
}

private fun weekDate(dayOffset: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
    return calendar.time
}

private fun isSameDay(first: Date, second: Date): Boolean {
    val a = Calendar.getInstance().apply { time = first }
    val b = Calendar.getInstance().apply { time = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun formattedTime(date: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

private fun formattedHours(seconds: Long): String {
    return String.format(Locale.getDefault(), "%.1fh", seconds / 3600.0)
}

private fun formattedDuration(seconds: Long): String {
    if (seconds <= 0L) return "--"
    val totalMinutes = seconds / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return "${hours}h ${"%02d".format(minutes)}m"
}

private fun formattedHistoryDate(date: Date): String {
    val weekday = SimpleDateFormat("EEE", Locale.getDefault())
        .format(date)
        .replace(".", "")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val calendar = Calendar.getInstance().apply { time = date }
    return "$weekday ${calendar.get(Calendar.DAY_OF_MONTH)}"
}
