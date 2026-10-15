package com.lognext.nexterandroid.features.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.GraphConsentRequiredException
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.CancellationException

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal fun LocationSearchGlyph(label: String) {
    val color = NexterColors.Red
    Canvas(Modifier.size(22.dp).semantics { contentDescription = label }) {
        drawCircle(color, radius = size.width * .28f, center = Offset(size.width * .4f, size.height * .4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, Offset(size.width * .6f, size.height * .6f), Offset(size.width * .9f, size.height * .9f), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
internal fun MeetingAvailabilityLabel(status: MeetingAvailability, loading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
            Text(stringResource(R.string.meeting_availability_loading), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText())
        } else {
            val color = when (status) {
                MeetingAvailability.Available -> NexterColors.systemGreen()
                MeetingAvailability.Tentative -> NexterColors.systemOrange()
                MeetingAvailability.Busy -> NexterColors.systemRed()
                MeetingAvailability.OutOfOffice -> NexterColors.systemPurple()
                MeetingAvailability.Unknown -> NexterColors.secondaryText()
            }
            val text = when (status) {
                MeetingAvailability.Available -> R.string.meeting_availability_available
                MeetingAvailability.Tentative -> R.string.meeting_availability_tentative
                MeetingAvailability.Busy -> R.string.meeting_availability_busy
                MeetingAvailability.OutOfOffice -> R.string.meeting_availability_oof
                MeetingAvailability.Unknown -> R.string.meeting_availability_unknown
            }
            MeetingStatusGlyph(status, color)
            Text(stringResource(text), style = MaterialTheme.typography.caption, color = color)
        }
    }
}

@Composable
private fun MeetingStatusGlyph(status: MeetingAvailability, color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width * .46f
        val stroke = 1.dp.toPx()
        if (status == MeetingAvailability.Unknown) {
            drawCircle(color, radius, center, style = Stroke(stroke))
            drawLine(color, Offset(size.width * .42f, size.height * .3f), Offset(size.width * .6f, size.height * .3f), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width * .6f, size.height * .3f), Offset(size.width * .5f, size.height * .55f), stroke, StrokeCap.Round)
            drawCircle(color, stroke * .55f, Offset(size.width * .5f, size.height * .75f))
        } else {
            drawCircle(color, radius, center)
            when (status) {
                MeetingAvailability.Available -> {
                    drawLine(Color.White, Offset(size.width * .25f, size.height * .5f), Offset(size.width * .43f, size.height * .68f), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(size.width * .43f, size.height * .68f), Offset(size.width * .76f, size.height * .32f), stroke, StrokeCap.Round)
                }
                MeetingAvailability.Tentative -> {
                    drawLine(Color.White, center, Offset(size.width * .5f, size.height * .22f), stroke, StrokeCap.Round)
                    drawLine(Color.White, center, Offset(size.width * .7f, size.height * .5f), stroke, StrokeCap.Round)
                }
                MeetingAvailability.Busy -> {
                    drawLine(Color.White, Offset(size.width * .5f, size.height * .22f), Offset(size.width * .5f, size.height * .55f), stroke, StrokeCap.Round)
                    drawCircle(Color.White, stroke * .6f, Offset(size.width * .5f, size.height * .75f))
                }
                MeetingAvailability.OutOfOffice -> drawLine(Color.White, Offset(size.width * .25f, size.height * .5f), Offset(size.width * .75f, size.height * .5f), stroke, StrokeCap.Round)
                else -> Unit
            }
        }
    }
}

@Composable
internal fun MeetingLocationDialog(initialQuery: String, start: Date, end: Date, onDismiss: () -> Unit, onSelect: (MeetingLocation) -> Unit) {
    var query by remember { mutableStateOf(initialQuery) }
    var catalog by remember { mutableStateOf<MeetingLocationCatalog?>(null) }
    var availability by remember { mutableStateOf(emptyMap<String, MeetingAvailability>()) }
    var checkingAvailability by remember { mutableStateOf(false) }
    var availabilityFailed by remember { mutableStateOf(false) }
    var availabilityConsent by remember { mutableStateOf(false) }
    var availabilityReload by remember { mutableStateOf(0) }
    var availabilityInteractive by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var consentRequired by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var interactive by remember { mutableStateOf(false) }
    val service = remember { MeetingGraphService() }
    val context = LocalContext.current
    LaunchedEffect(reload) {
        loading = true; consentRequired = false; error = null
        try {
            val activity = if (interactive) context.findActivity() else null
            interactive = false
            val token = AppDependencies.authRepository.graphReadToken("Place.Read.All", activity)
            catalog = service.locations(token)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: GraphConsentRequiredException) { consentRequired = true }
        catch (failure: Exception) {
            error = context.getString(if (failure is MeetingGraphHttpException && failure.status == 403) R.string.meeting_locations_forbidden else R.string.meeting_locations_error)
        } finally { loading = false }
    }
    LaunchedEffect(catalog, start.time, end.time, availabilityReload) {
        availability = emptyMap(); availabilityFailed = false; availabilityConsent = false; checkingAvailability = false
        val emails = catalog?.locations.orEmpty().filter { it.kind == MeetingLocationKind.Room }.mapNotNull { it.emailAddress?.trim()?.takeIf(String::isNotEmpty) }
        if (emails.isEmpty()) return@LaunchedEffect
        checkingAvailability = true
        try {
            val activity = if (availabilityInteractive) context.findActivity() else null
            availabilityInteractive = false
            val token = AppDependencies.authRepository.graphReadToken("Calendars.Read", activity)
            availability = service.availability(token, emails, start, end)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: GraphConsentRequiredException) { availabilityConsent = true; availabilityFailed = true }
        catch (_: Exception) { availabilityFailed = true }
        finally { checkingAvailability = false }
    }
    MeetingCreationFrame(false, onDismiss, rememberScrollState(), title = stringResource(R.string.meeting_locations_title)) { _ ->
        MeetingFormSection { MeetingFormText(query, { query = it }, stringResource(R.string.meeting_locations_hint), true) }
        if (loading) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.meeting_locations_loading), color = NexterColors.secondaryText())
        }
        if (consentRequired) {
            Text(stringResource(R.string.meeting_locations_consent), color = NexterColors.secondaryText())
            TextButton(onClick = { interactive = true; reload++ }) { Text(stringResource(R.string.meeting_locations_authorize)) }
        }
        error?.let {
            Text(it, color = NexterColors.secondaryText())
            TextButton(onClick = { reload++ }) { Text(stringResource(R.string.retry)) }
        }
        catalog?.let { loaded ->
            Text(stringResource(R.string.meeting_availability_notice), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
            if (availabilityFailed) {
                Text(stringResource(if (availabilityConsent) R.string.meeting_availability_consent else R.string.meeting_availability_error), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                TextButton(onClick = { availabilityInteractive = availabilityConsent; availabilityReload++ }, enabled = !checkingAvailability) {
                    Text(stringResource(if (availabilityConsent) R.string.meeting_availability_authorize else R.string.meeting_availability_retry))
                }
            }
            val matches = loaded.locations.filter { it.matches(query) }
            for (kind in MeetingLocationKind.values()) {
                val group = matches.filter { it.kind == kind }
                if (group.isNotEmpty()) MeetingFormSection(stringResource(when (kind) {
                    MeetingLocationKind.Room -> R.string.meeting_locations_rooms
                    MeetingLocationKind.Office -> R.string.meeting_locations_offices
                    MeetingLocationKind.RoomGroup -> R.string.meeting_locations_groups
                })) {
                    group.forEachIndexed { index, place ->
                        if (index > 0) MeetingFormDivider()
                        Column(Modifier.fillMaxWidth().clickable { onSelect(place) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(place.displayName, color = NexterColors.primaryText(), modifier = Modifier.weight(1f))
                                if (place.kind == MeetingLocationKind.Room) {
                                    RoomAvailabilityBadge(availability[place.emailAddress?.trim()?.lowercase(Locale.ROOT)] ?: MeetingAvailability.Unknown, checkingAvailability)
                                }
                            }
                            place.capacity?.takeIf { it > 0 }?.let { Text(stringResource(R.string.meeting_locations_capacity, it), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption) }
                        }
                    }
                }
            }
            if (matches.isEmpty()) Text(stringResource(if (loaded.locations.isEmpty()) R.string.meeting_locations_empty else R.string.meeting_locations_no_matches), color = NexterColors.secondaryText())
            if (loaded.isPartial) Text(stringResource(R.string.meeting_locations_partial), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
        }
    }
}


@Composable
private fun RoomAvailabilityBadge(status: MeetingAvailability, loading: Boolean) {
    val color = when {
        status == MeetingAvailability.Available -> NexterColors.systemGreen()
        status.hasConflict -> NexterColors.systemRed()
        else -> NexterColors.secondaryText()
    }
    val text = when {
        loading -> R.string.meeting_availability_loading
        status == MeetingAvailability.Available -> R.string.meeting_availability_available
        status.hasConflict -> R.string.meeting_room_occupied_badge
        else -> R.string.meeting_room_unknown_badge
    }
    Text(stringResource(text), color = color, style = MaterialTheme.typography.caption,
        modifier = Modifier.widthIn(max = 145.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp))
}
