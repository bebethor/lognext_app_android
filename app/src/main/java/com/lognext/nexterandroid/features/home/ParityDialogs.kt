package com.lognext.nexterandroid.features.home

import com.lognext.nexterandroid.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalFocusManager
import com.lognext.nexterandroid.core.auth.GraphConsentRequiredException
import com.lognext.nexterandroid.ui.theme.NexterTypography
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.features.people.*
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun DetailFrame(title: String, busy: Boolean = false, onDismiss: () -> Unit, scrollState: ScrollState? = null, body: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = NexterColors.cardBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.h6)
                    TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.parity_close)) }
                }
                Column(Modifier.weight(1f).verticalScroll(scrollState ?: rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = body)
            }
        }
    }
}

@Composable
internal fun MeetingDetailDialog(meeting: HomeCalendarEvent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var loadingEmail by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val people = remember { PeopleViewModel(AppDependencies.peopleService) }
    DisposableEffect(people) { onDispose { people.dismissSelectedPerson() } }
    DetailFrame(stringResource(R.string.parity_meeting), onDismiss = onDismiss) {
        Text(meeting.subject, style = MaterialTheme.typography.h6)
        meeting.startDate?.let { Text(stringResource(R.string.parity_start_value, DateFormat.getDateTimeInstance().format(it))) }
        meeting.endDate?.let { Text(stringResource(R.string.parity_end_value, DateFormat.getDateTimeInstance().format(it))) }
        if (meeting.isOnlineMeeting) Text(stringResource(R.string.parity_online_teams))
        meeting.organizer?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.parity_organizer_value, it)) }
        meeting.location?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.parity_location_value, it)) }
        meeting.roomReservation?.let { reservation ->
            MeetingFormSection(stringResource(R.string.meeting_room_booking)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(reservation.name.ifBlank { reservation.email }, color = NexterColors.primaryText())
                    Text(stringResource(reservation.titleResource()), color = when (reservation.status) {
                        "accepted" -> NexterColors.systemGreen(); "declined" -> NexterColors.systemRed(); else -> NexterColors.systemOrange()
                    })
                    Text(stringResource(reservation.messageResource()), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                }
            }
        }
        val attendees = meeting.attendees.orEmpty()
        TextButton(onClick = { expanded = !expanded }, enabled = attendees.isNotEmpty()) {
            Text(stringResource(R.string.parity_attendees_value, meeting.attendeesCount ?: attendees.size, if (expanded) "▴" else "▾"))
        }
        if (expanded) attendees.forEach { attendee ->
            Row(Modifier.fillMaxWidth().clickable(enabled = loadingEmail == null && attendee.email.isNotBlank()) {
                scope.launch {
                    loadingEmail = attendee.email
                    error = null
                    try {
                        val query = attendee.name.ifBlank { attendee.email.substringBefore('@') }
                        val matches = AppDependencies.peopleService.searchStaffSmart(query).people
                            .filter { it.email.orEmpty().trim().equals(attendee.email.trim(), ignoreCase = true) && it.personCode.orEmpty().isNotBlank() }
                        if (matches.size == 1) people.selectPerson(matches.single().toPeopleRow())
                        else error = context.getString(R.string.parity_no_profile)
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { error = context.getString(R.string.parity_profile_error) }
                    finally { loadingEmail = null }
                }
            }.padding(8.dp)) {
                Column(Modifier.weight(1f)) { Text(attendee.name.ifBlank { attendee.email }); Text(attendee.email, style = MaterialTheme.typography.caption) }
                if (loadingEmail == attendee.email) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        error?.let { Text(it, color = NexterColors.systemRed()) }
        meeting.joinUrl?.let { url ->
            val uri = Uri.parse(url)
            if (uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()) {
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }) { Text(stringResource(R.string.parity_join)) }
            }
        }
    }
    people.selectedPerson?.let { person ->
        PersonDetailDialog(person, people.managerFor(person), people.reportsFor(person), people.isLoadingDetail, people.detailErrorMessage, people::selectPerson, people::dismissSelectedPerson, onRefresh = { AppDependencies.clearResponseCache(); people.selectPerson(person) })
    }
}

@Composable
private fun PriorityDropdown(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val priorityColors = mapOf("high" to NexterColors.systemRed(), "low" to NexterColors.systemBlue(), "normal" to NexterColors.systemOrange())
    fun color(key: String) = priorityColors[key] ?: priorityColors.getValue("normal")
    val titles = mapOf("high" to stringResource(R.string.priority_high), "low" to stringResource(R.string.priority_low), "normal" to stringResource(R.string.priority_normal))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.parity_priority))
        Box {
            TextButton(onClick = { expanded = true }, enabled = enabled) { Text("${titles[value]} ▾", color = color(value)) }
            DropdownMenu(expanded, { expanded = false }) {
                listOf("high", "normal", "low").forEach { key -> DropdownMenuItem(onClick = { onChange(key); expanded = false }) { Text(titles[key].orEmpty(), color = color(key)) } }
            }
        }
    }
}

@Composable
internal fun TaskEditorDialog(task: HomeTask, onDismiss: () -> Unit, onSave: suspend (String, String, String, String?) -> Unit) {
    var title by remember { mutableStateOf(task.displayTitle) }
    var description by remember { mutableStateOf(task.description.orEmpty()) }
    var priority by remember { mutableStateOf(task.priorityKey) }
    var due by remember { mutableStateOf(task.sortDueDate) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DetailFrame(stringResource(R.string.parity_task_details), busy, onDismiss) {
        TextField(title, { title = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), label = { Text(stringResource(R.string.parity_title)) }, modifier = Modifier.fillMaxWidth())
        TextField(description, { description = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), label = { Text(stringResource(R.string.parity_description)) }, modifier = Modifier.fillMaxWidth())
        PriorityDropdown(priority, !busy) { priority = it }
        task.creationDate?.let { Text(stringResource(R.string.parity_created_value, DateFormat.getDateTimeInstance().format(it))) }
        TextButton(onClick = { pickDateTime(context, due ?: Date()) { due = it } }, enabled = !busy) { Text(due?.let { context.getString(R.string.parity_due_value, DateFormat.getDateTimeInstance().format(it)) } ?: context.getString(R.string.parity_add_due)) }
        if (due != null) TextButton(onClick = { due = null }, enabled = !busy) { Text(stringResource(R.string.parity_remove_due)) }
        error?.let { Text(it, color = NexterColors.systemRed()) }
        Button(enabled = title.isNotBlank() && !busy, onClick = {
            scope.launch {
                busy = true
                error = null
                try { onSave(title.trim(), description, priority, due?.let(::apiDate)); onDismiss() }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { error = context.getString(R.string.parity_save_error) }
                finally { busy = false }
            }
        }) { if (busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text(stringResource(R.string.parity_save)) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun CreateMeetingDialog(onDismiss: () -> Unit, onCreate: suspend (CreateMeetingRequest) -> HomeCalendarEvent) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(Date()) }
    var end by remember { mutableStateOf(Date(System.currentTimeMillis() + 3600000)) }
    var online by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var attendees by remember { mutableStateOf(emptyList<MeetingAttendee>()) }
    var results by remember { mutableStateOf(emptyList<PersonSummaryResponse>()) }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchFocused by remember { mutableStateOf(false) }
    val searchRequester = remember { BringIntoViewRequester() }
    var resultsQuery by remember { mutableStateOf("") }
    var showLocations by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<MeetingLocation?>(null) }
    var roomAvailability by remember { mutableStateOf(MeetingAvailability.Unknown) }
    var checkingRoom by remember { mutableStateOf(false) }
    var roomError by remember { mutableStateOf<String?>(null) }
    var roomReload by remember { mutableStateOf(0) }
    var roomConsentRequired by remember { mutableStateOf(false) }
    var roomInteractive by remember { mutableStateOf(false) }
    var bookingResult by remember { mutableStateOf<RoomReservation?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var availability by remember { mutableStateOf(emptyMap<String, MeetingAvailability>()) }
    var checkingAvailability by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf<String?>(null) }
    var availabilityConsentRequired by remember { mutableStateOf(false) }
    var availabilityReload by remember { mutableStateOf(0) }
    var availabilityInteractive by remember { mutableStateOf(false) }
    val graphService = remember { MeetingGraphService() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedRoom = selectedLocation?.takeIf { it.kind == MeetingLocationKind.Room }?.let { place ->
        place.emailAddress?.trim()?.takeIf { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }?.let { MeetingRoom(it, place.displayName) }
    }
    val fingerprint = listOf(title, description, location, start.time, end.time, online, attendees, selectedRoom?.email).toString()
    val roomFingerprint = listOf(selectedRoom?.email, start.time, end.time, roomReload).toString()
    LaunchedEffect(roomFingerprint) {
        roomAvailability = MeetingAvailability.Unknown
        roomError = null; roomConsentRequired = false; checkingRoom = false
        if (selectedRoom == null) return@LaunchedEffect
        if (!start.before(end) || end.time - start.time >= 62L * 24 * 60 * 60 * 1000) {
            roomError = context.getString(R.string.meeting_availability_interval)
            return@LaunchedEffect
        }
        checkingRoom = true
        try {
            delay(400)
            val activity = if (roomInteractive) context.findActivity() else null
            roomInteractive = false
            val token = AppDependencies.authRepository.graphReadToken("Calendars.Read", activity)
            val statuses = graphService.availability(token, listOf(selectedRoom.email), start, end)
            roomAvailability = statuses[selectedRoom.email.lowercase(Locale.ROOT)] ?: MeetingAvailability.Unknown
            if (roomAvailability == MeetingAvailability.Unknown) roomError = context.getString(R.string.meeting_room_unknown)
            checkingRoom = false
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: GraphConsentRequiredException) {
            checkingRoom = false; roomConsentRequired = true
            roomError = context.getString(R.string.meeting_availability_consent)
        } catch (_: Exception) {
            checkingRoom = false
            roomError = context.getString(R.string.meeting_room_unknown)
        }
    }
    val transactionId = remember(fingerprint) { UUID.randomUUID().toString() }
    val initialFingerprint = remember { fingerprint }
    val requestDismiss = {
        focusManager.clearFocus()
        if (fingerprint != initialFingerprint || query.isNotEmpty()) showDiscardConfirmation = true else onDismiss()
    }
    val availabilityFingerprint = listOf(start.time, end.time, attendees.map { it.email.lowercase(Locale.ROOT) }.sorted(), availabilityReload).toString()
    LaunchedEffect(availabilityFingerprint) {
        availability = emptyMap()
        availabilityError = null
        availabilityConsentRequired = false
        checkingAvailability = false
        if (attendees.isEmpty()) return@LaunchedEffect
        if (!start.before(end) || end.time - start.time >= 62L * 24 * 60 * 60 * 1000) {
            availabilityError = context.getString(R.string.meeting_availability_interval)
            return@LaunchedEffect
        }
        checkingAvailability = true
        try {
            delay(400)
            val activity = if (availabilityInteractive) context.findActivity() else null
            availabilityInteractive = false
            val token = AppDependencies.authRepository.graphReadToken("Calendars.Read", activity)
            availability = graphService.availability(token, attendees.map { it.email }, start, end)
            checkingAvailability = false
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: GraphConsentRequiredException) {
            checkingAvailability = false
            availabilityConsentRequired = true
            availabilityError = context.getString(R.string.meeting_availability_consent)
        } catch (_: Exception) {
            checkingAvailability = false
            availabilityError = context.getString(R.string.meeting_availability_error)
        }
    }
    fun add(person: MeetingAttendee) {
        val clean = person.copy(email = person.email.trim())
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(clean.email).matches() && attendees.none { it.email.equals(clean.email, true) }) {
            attendees = attendees + clean; query = ""; results = emptyList()
        }
    }
    LaunchedEffect(query) {
        searching = false
        hasSearched = false
        searchError = null
        val searchQuery = query.trim()
        if (searchQuery.length < 2 || searchQuery.contains('@')) {
            results = emptyList()
            resultsQuery = ""
            return@LaunchedEffect
        }
        searching = true
        try {
            delay(300)
            val found = AppDependencies.peopleService.searchStaffSmart(searchQuery).people
            coroutineContext.ensureActive()
            results = found.filter { person -> attendees.none { it.email.equals(person.email.orEmpty(), true) } }
            resultsQuery = searchQuery
            hasSearched = true
            searching = false
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            searching = false
            searchError = context.getString(R.string.parity_search_error)
        }
    }
    LaunchedEffect(searchFocused, keyboardHeight, results.size, searching, checkingAvailability, attendees.size) {
        if (searchFocused && keyboardHeight > 0) {
            delay(100)
            searchRequester.bringIntoView()
        }
    }
    if (showDiscardConfirmation) AlertDialog(onDismissRequest = { showDiscardConfirmation = false },
        title = { Text(stringResource(R.string.meeting_discard_title)) },
        text = { Text(stringResource(R.string.meeting_discard_message)) },
        confirmButton = { TextButton(onClick = { showDiscardConfirmation = false; onDismiss() }) { Text(stringResource(R.string.meeting_discard), color = NexterColors.systemRed()) } },
        dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) { Text(stringResource(R.string.meeting_keep_editing)) } })
    if (showLocations) MeetingLocationDialog(location, onDismiss = { showLocations = false }) {
        selectedLocation = it
        location = it.locationName
        showLocations = false
    }
    bookingResult?.let { reservation ->
        AlertDialog(onDismissRequest = { onDismiss() }, title = { Text(stringResource(reservation.titleResource())) },
            text = { Text(stringResource(reservation.messageResource())) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.parity_close)) } })
    }
    val fontScale = LocalDensity.current.fontScale
    MeetingCreationFrame(busy, requestDismiss, scrollState) { viewportHeight ->
        MeetingFormSection(stringResource(R.string.parity_meeting)) {
            MeetingFormText(title, { title = it }, stringResource(R.string.parity_title), !busy)
            MeetingFormDivider()
            MeetingDateRow(stringResource(R.string.meeting_start_label), start, !busy) { pickDateTime(context, start) { start = it } }
            MeetingFormDivider()
            MeetingDateRow(stringResource(R.string.meeting_end_label), end, !busy) { pickDateTime(context, end) { end = it } }
            Text(TimeZone.getDefault().displayName, color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            MeetingFormDivider()
            MeetingFormText(description, { description = it }, stringResource(R.string.parity_description), !busy, multiline = true)
            MeetingFormDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.parity_teams), modifier = Modifier.weight(1f), color = NexterColors.primaryText())
                val isDark = com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()
                Switch(
                    checked = online,
                    onCheckedChange = { online = it },
                    enabled = !busy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NexterColors.Red,
                        checkedTrackAlpha = 1f,
                        uncheckedThumbColor = if (isDark) Color(0xFFD0D2DF) else Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF6B6E80) else Color(0xFFA4A5AF),
                        uncheckedTrackAlpha = 1f
                    )
                )
            }
            MeetingFormDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { MeetingFormText(location, { location = it; if (selectedLocation?.locationName != it) selectedLocation = null }, stringResource(R.string.parity_location), !busy) }
                val searchLabel = stringResource(R.string.meeting_locations_search)
                IconButton(onClick = { focusManager.clearFocus(); showLocations = true }, enabled = !busy) { LocationSearchGlyph(searchLabel) }
            }
        }
        selectedLocation?.let { place ->
            MeetingFormSection(stringResource(R.string.meeting_room_booking)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (place.kind == MeetingLocationKind.Room) {
                        if (selectedRoom == null) Text(stringResource(R.string.meeting_room_no_mailbox), color = NexterColors.secondaryText())
                        else {
                            MeetingAvailabilityLabel(roomAvailability, checkingRoom)
                            if (roomAvailability.hasConflict) Text(stringResource(R.string.meeting_room_busy), color = NexterColors.systemRed())
                            roomError?.let {
                                Text(it, color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                                TextButton(onClick = { roomInteractive = roomConsentRequired; roomReload++ }) {
                                    Text(stringResource(if (roomConsentRequired) R.string.meeting_availability_authorize else R.string.meeting_availability_retry))
                                }
                            }
                        }
                        Text(stringResource(R.string.meeting_room_confirmation_notice), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                    } else Text(stringResource(R.string.meeting_room_office_notice), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                }
            }
        }
        if (selectedLocation == null && location.isNotBlank()) Text(stringResource(R.string.meeting_room_manual_notice), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
        MeetingFormSection(stringResource(R.string.parity_attendees)) {
            attendees.forEach { attendee ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(attendee.name.ifBlank { attendee.email }, color = NexterColors.primaryText())
                        MeetingAvailabilityLabel(availability[attendee.email.lowercase(Locale.ROOT)] ?: MeetingAvailability.Unknown, checkingAvailability)
                    }
                    val removeLabel = stringResource(R.string.parity_remove)
                    Box(Modifier.padding(start = 12.dp).size(28.dp).border(BorderStroke(1.dp, NexterColors.Red), CircleShape)
                        .clickable(enabled = !busy) { attendees = attendees - attendee }.semantics { contentDescription = removeLabel }, contentAlignment = Alignment.Center) { Text("−", color = NexterColors.Red) }
                }
                MeetingFormDivider()
            }
            Column(Modifier.fillMaxWidth().bringIntoViewRequester(searchRequester)) {
                MeetingFormText(query, { query = it }, stringResource(R.string.parity_attendee_input), !busy,
                    modifier = Modifier.onFocusChanged { searchFocused = it.isFocused }, attendeeSearch = true)
                if (query.contains('@')) {
                    MeetingFormDivider()
                    TextButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && android.util.Patterns.EMAIL_ADDRESS.matcher(query.trim()).matches(), onClick = { add(MeetingAttendee(email = query)) }) { Text(stringResource(R.string.parity_add_email)) }
                } else if (searchFocused || query.isNotBlank()) {
                    if (searching) Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.meeting_searching), color = NexterColors.secondaryText())
                    } else if (query.trim().length < 2) Text(stringResource(R.string.meeting_search_hint), style = MaterialTheme.typography.caption,
                        color = NexterColors.secondaryText(), modifier = Modifier.padding(16.dp))
                    searchError?.let { Text(it, color = NexterColors.systemRed(), modifier = Modifier.padding(16.dp)) }
                    if (hasSearched && results.isEmpty() && searchError == null) Text(stringResource(R.string.meeting_no_matches), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText(), modifier = Modifier.padding(16.dp))
                    if (results.isNotEmpty()) {
                        val rowHeight = (62 * fontScale).dp
                        val panelHeight = (rowHeight * results.size).coerceAtMost((viewportHeight - (140 * fontScale).dp).coerceAtLeast(rowHeight))
                        LazyColumn(Modifier.fillMaxWidth().height(panelHeight)) {
                            items(results) { person ->
                                Column(Modifier.fillMaxWidth().height(rowHeight).clickable(enabled = !busy && !searching && resultsQuery == query.trim() && android.util.Patterns.EMAIL_ADDRESS.matcher(person.email.orEmpty()).matches()) {
                                    add(MeetingAttendee(person.fullName.orEmpty(), person.email.orEmpty()))
                                }.padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
                                    Text(person.fullName.orEmpty(), color = NexterColors.Red, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(person.email.orEmpty(), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (attendees.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.meeting_availability_notice), color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                if (availability.values.any { it.hasConflict }) Text(stringResource(R.string.meeting_availability_conflict), color = NexterColors.systemOrange(), style = MaterialTheme.typography.caption)
                availabilityError?.let {
                    Text(it, color = NexterColors.secondaryText(), style = MaterialTheme.typography.caption)
                    TextButton(onClick = { availabilityInteractive = availabilityConsentRequired; availabilityReload++ }, enabled = !busy) {
                        Text(stringResource(if (availabilityConsentRequired) R.string.meeting_availability_authorize else R.string.meeting_availability_retry))
                    }
                }
            }
        }
        error?.let { MeetingFormSection { Text(it, color = NexterColors.systemRed(), modifier = Modifier.padding(16.dp)) } }
        MeetingFormSection {
        TextButton(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), enabled = title.isNotBlank() && end.after(start) && !busy && (online || location.isNotBlank())
            && (selectedLocation?.kind != MeetingLocationKind.Room || (selectedRoom != null && roomAvailability == MeetingAvailability.Available && !checkingRoom)), onClick = {
            scope.launch {
                busy = true; error = null
                try {
                    val activity = context.findActivity() ?: error("No hay una actividad disponible")
                    AppDependencies.authRepository.requestCalendarWritePermission(activity)
                    if (selectedRoom != null) {
                        val token = AppDependencies.authRepository.graphReadToken("Calendars.Read")
                        val statuses = graphService.availability(token, listOf(selectedRoom.email), start, end)
                        val fresh = statuses[selectedRoom.email.lowercase(Locale.ROOT)] ?: MeetingAvailability.Unknown
                        if (fresh != MeetingAvailability.Available) {
                            roomAvailability = fresh
                            error = context.getString(R.string.meeting_room_changed)
                            return@launch
                        }
                    }
                    val created = onCreate(CreateMeetingRequest(title.trim(), apiDate(start), apiDate(end), description, location, online, attendees, transactionId, selectedRoom))
                    if (selectedRoom != null) bookingResult = created.roomReservation ?: RoomReservation(selectedRoom.email, selectedRoom.name, "pending")
                    else onDismiss()
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { error = if (failure is RoomBookingUnavailableException) context.getString(R.string.meeting_room_backend_unavailable) else failure.message ?: context.getString(R.string.parity_create_error) }
                finally { busy = false }
            }
        }) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.parity_create_send).uppercase(Locale.getDefault()))
        }
        }
        Text(stringResource(R.string.meeting_location_required), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText(), modifier = Modifier.padding(horizontal = 16.dp))
        Text(stringResource(R.string.parity_invite_notice), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText(), modifier = Modifier.padding(horizontal = 16.dp))
    }
}

private fun apiDate(date: Date): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)
private fun pickDateTime(context: android.content.Context, initial: Date, onPicked: (Date) -> Unit) {
    val calendar = Calendar.getInstance().apply { time = initial }
    DatePickerDialog(context, { _, year, month, day ->
        calendar.set(year, month, day)
        TimePickerDialog(context, { _, hour, minute -> calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); onPicked(calendar.time) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(context)).show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
}
