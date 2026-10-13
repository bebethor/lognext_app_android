package com.lognext.nexterandroid.features.home

import com.lognext.nexterandroid.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
internal fun CreateMeetingDialog(onDismiss: () -> Unit, onCreate: suspend (CreateMeetingRequest) -> Unit) {
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
    val submitRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fingerprint = listOf(title, description, location, start.time, end.time, online, attendees).toString()
    val transactionId = remember(fingerprint) { UUID.randomUUID().toString() }
    fun add(person: MeetingAttendee) {
        val clean = person.copy(email = person.email.trim())
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(clean.email).matches() && attendees.none { it.email.equals(clean.email, true) }) {
            attendees = attendees + clean; query = ""; results = emptyList()
        }
    }
    LaunchedEffect(query) {
        results = emptyList()
        searching = false
        hasSearched = false
        searchError = null
        val searchQuery = query.trim()
        if (searchQuery.length < 2 || searchQuery.contains('@')) return@LaunchedEffect
        try {
            delay(300)
            searching = true
            val found = AppDependencies.peopleService.searchStaffSmart(searchQuery).people
            coroutineContext.ensureActive()
            results = found.filter { person -> attendees.none { it.email.equals(person.email.orEmpty(), true) } }
            hasSearched = true
            searching = false
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            searching = false
            searchError = context.getString(R.string.parity_search_error)
        }
    }
    LaunchedEffect(searchFocused, keyboardHeight, results.size) {
        if (searchFocused && keyboardHeight > 0) {
            delay(100)
            submitRequester.bringIntoView()
        }
    }
    DetailFrame(stringResource(R.string.parity_new_meeting), busy, onDismiss, scrollState) {
        TextField(title, { title = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.parity_title)) })
        TextButton(enabled = !busy, onClick = { pickDateTime(context, start) { start = it } }) { Text(stringResource(R.string.parity_start_value, DateFormat.getDateTimeInstance().format(start))) }
        TextButton(enabled = !busy, onClick = { pickDateTime(context, end) { end = it } }) { Text(stringResource(R.string.parity_end_value, DateFormat.getDateTimeInstance().format(end))) }
        Text(TimeZone.getDefault().displayName, style = MaterialTheme.typography.caption)
        TextField(description, { description = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.parity_description)) })
        TextField(location, { location = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.parity_location)) })
        Row { Text(stringResource(R.string.parity_teams)); Switch(online, { online = it }, enabled = !busy) }
        Text(stringResource(R.string.parity_attendees))
        attendees.forEach { attendee -> Row { Text(attendee.name.ifBlank { attendee.email }, modifier = Modifier.weight(1f)); TextButton(enabled = !busy, onClick = { attendees = attendees - attendee }) { Text(stringResource(R.string.parity_remove)) } } }
        TextField(query, { query = it }, enabled = !busy, colors = com.lognext.nexterandroid.ui.theme.nexterTextFieldColors(), modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused }, label = { Text(stringResource(R.string.parity_attendee_input)) })
        if (query.contains('@')) TextButton(enabled = !busy && android.util.Patterns.EMAIL_ADDRESS.matcher(query.trim()).matches(), onClick = { add(MeetingAttendee(email = query)) }) { Text(stringResource(R.string.parity_add_email)) }
        if (searching) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.meeting_searching))
        }
        searchError?.let { Text(it, color = NexterColors.systemRed()) }
        if (hasSearched && results.isEmpty() && searchError == null) Text(stringResource(R.string.meeting_no_matches), style = MaterialTheme.typography.caption)
        results.forEach { person -> TextButton(enabled = !busy, onClick = { add(MeetingAttendee(person.fullName.orEmpty(), person.email.orEmpty())) }) { Text("${person.fullName} · ${person.email}") } }
        error?.let { Text(it, color = NexterColors.systemRed()) }
        Text(stringResource(R.string.parity_invite_notice), style = MaterialTheme.typography.caption)
        Button(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(submitRequester), enabled = title.isNotBlank() && end.after(start) && !busy, onClick = {
            scope.launch {
                busy = true; error = null
                try {
                    val activity = context as? Activity ?: error("No hay una actividad disponible")
                    AppDependencies.authRepository.requestCalendarWritePermission(activity)
                    onCreate(CreateMeetingRequest(title.trim(), apiDate(start), apiDate(end), description, location, online, attendees, transactionId))
                    onDismiss()
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { error = failure.message ?: context.getString(R.string.parity_create_error) }
                finally { busy = false }
            }
        }) { if (busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text(stringResource(R.string.parity_create_send).uppercase(Locale.getDefault())) }
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
