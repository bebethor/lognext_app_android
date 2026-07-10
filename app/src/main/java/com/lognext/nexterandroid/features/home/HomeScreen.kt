package com.lognext.nexterandroid.features.home

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.AuthState
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import com.lognext.nexterandroid.ui.theme.isNexterDarkTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeScreen() {
    val authRepository = AppDependencies.authRepository
    val authState by authRepository.authState.collectAsState()
    val isLoggingIn by authRepository.isLoggingIn.collectAsState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(authRepository) {
        authRepository.restoreSession()
    }

    when (val state = authState) {
        AuthState.Loading -> CenteredHomeShell {
            CircularProgressIndicator(color = NexterColors.Red)
        }
        AuthState.Unauthenticated -> LoginScreen(
            isSigningIn = isLoggingIn,
            errorMessage = null,
            onSignIn = {
                activity?.let { scope.launch { authRepository.signIn(it) } }
            }
        )
        is AuthState.Authenticated -> {
            AuthenticatedHome(
                authState = state
            )
        }
        is AuthState.Error -> LoginScreen(
            isSigningIn = isLoggingIn,
            errorMessage = state.message,
            onSignIn = {
                activity?.let { scope.launch { authRepository.signIn(it) } }
            }
        )
    }
}

@Composable
private fun LoginScreen(
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit
) {
    val transition = rememberInfiniteTransition()
    val isDark = isNexterDarkTheme()
    val logoRes = if (isDark) R.drawable.lognext_logo_negative else R.drawable.lognext_logo
    val textAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) NexterColors.DarkPageBackground else Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_bottom_wave),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Lognext",
                modifier = Modifier
                    .width(235.dp)
                    .height(54.dp)
                    .offset(y = (-100).dp)
            )

            Spacer(modifier = Modifier.height(0.dp))

            Button(
                enabled = !isSigningIn,
                onClick = onSignIn,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = NexterColors.primaryText()
                ),
                elevation = null,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(2.dp, NexterColors.Red),
                modifier = Modifier
                    .width(255.dp)
                    .height(58.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        color = NexterColors.primaryText(),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        color = NexterColors.primaryText(),
                        fontSize = NexterTypography.Button,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = NexterColors.Red,
                    fontSize = NexterTypography.Caption,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            Column(
                modifier = Modifier.alpha(textAlpha),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LoginClaimLine(
                    red = stringResource(R.string.login_claim_technology_red),
                    navy = stringResource(R.string.login_claim_technology_text)
                )
                LoginClaimLine(
                    red = stringResource(R.string.login_claim_commitment_red),
                    navy = stringResource(R.string.login_claim_commitment_text)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LoginClaimLine(red: String, navy: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = NexterColors.Red)) { append(red) }
            withStyle(SpanStyle(color = NexterColors.primaryText())) { append(navy) }
        },
        fontSize = NexterTypography.CardTitle,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun AuthenticatedHome(
    authState: AuthState.Authenticated
) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(AppDependencies.homeService) as T
            }
        }
    }
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    var showAgenda by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }

    LaunchedEffect(authState.user.username) {
        viewModel.loadIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 80.dp)
        ) {
            GreetingCard(uiState, authState.user.displayName)
            if (uiState.isLoading) LoadingCard()
            uiState.errorMessage?.let {
                ErrorCard(message = it, onRetry = viewModel::refresh)
                Spacer(modifier = Modifier.height(10.dp))
            }
            MeetingsCard(
                uiState = uiState,
                onOpenAgenda = {
                    viewModel.loadAgenda(AgendaScope.Today)
                    showAgenda = true
                }
            )
            TasksCard(
                uiState = uiState,
                onAddTask = { showAddTask = true },
                onToggleCompleted = viewModel::toggleCompleted,
                onDeleteTask = viewModel::deleteTask
            )
        }
    }

    if (showAgenda) {
        AgendaDialog(
            uiState = uiState,
            onDismiss = { showAgenda = false },
            onScopeSelected = { viewModel.loadAgenda(it, force = uiState.agendaScope != it) },
            onRetry = { viewModel.loadAgenda(uiState.agendaScope, force = true) }
        )
    }

    if (showAddTask) {
        AddTaskDialog(
            uiState = uiState,
            onDismiss = { showAddTask = false },
            onCreate = { title, description, priority, dueDate ->
                viewModel.createTask(title, description, priority, dueDate)
                if (uiState.createTaskErrorMessage == null) showAddTask = false
            }
        )
    }
}

@Composable
private fun GreetingCard(uiState: HomeUiState, displayName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NexterColors.Navy)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = (-52).dp)
                .size(130.dp)
                .rotate(-30f)
                .border(BorderStroke(2.dp, NexterColors.Red.copy(alpha = 0.25f)), RoundedCornerShape(4.dp))
        )

        Column {
            Text(stringResource(R.string.home_welcome), color = Color.White.copy(alpha = 0.70f), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold)
            Text(
                text = uiState.firstName.ifBlank { displayName },
                color = Color.White,
                fontSize = NexterTypography.CardTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 7.dp)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NexterColors.Red.copy(alpha = 0.20f))
                    .border(BorderStroke(1.dp, NexterColors.Red.copy(alpha = 0.40f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NexterColors.Red))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.home_loading), color = Color.White.copy(alpha = 0.80f), fontSize = NexterTypography.Badge, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                GreetingStat(uiState.formattedVacationDays, stringResource(R.string.home_available_days_short), Modifier.weight(1f))
                GreetingDivider()
                GreetingStat((uiState.meetingsTodayCount ?: uiState.todayMeetings.size).toString(), stringResource(R.string.home_meetings_today_short), Modifier.weight(1f))
                GreetingDivider()
                GreetingStat(uiState.displayedPendingTasksCount.toString(), stringResource(R.string.home_pending_tasks_short), Modifier.weight(1f))
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun GreetingStat(number: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, color = Color.White, fontSize = NexterTypography.Metric, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.70f), fontSize = NexterTypography.Caption, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GreetingDivider() {
    Box(
        modifier = Modifier
            .height(42.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@Composable
private fun MeetingsCard(uiState: HomeUiState, onOpenAgenda: () -> Unit) {
    HtmlCard(title = "📅", label = stringResource(R.string.home_today_meetings), action = stringResource(R.string.home_view_agenda), onAction = onOpenAgenda) {
        when {
            uiState.isLoading -> CardStateMessage(stringResource(R.string.home_loading_meetings), showProgress = true)
            uiState.visibleMeetings.isEmpty() -> EmptyText(
                text = stringResource(R.string.home_no_meetings_today),
                subtitle = stringResource(R.string.home_no_meetings_subtitle)
            )
            else -> uiState.visibleMeetings.forEachIndexed { index, meeting ->
                if (index > 0) Divider(color = NexterColors.border())
                MeetingRow(meeting, index)
            }
        }
    }
}

@Composable
private fun MeetingRow(meeting: HomeCalendarEvent, index: Int) {
    val colors = listOf(NexterColors.Red, NexterColors.Blue, NexterColors.Green, NexterColors.Violet, NexterColors.Aqua)
    Row(modifier = Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = meeting.startTimeText,
            color = NexterColors.tertiaryText(),
            fontSize = NexterTypography.Footnote,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors[index % colors.size])
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(meeting.subject, color = NexterColors.primaryText(), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(meeting.subtitle, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Footnote, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        meeting.tagText?.let {
            Text(
                text = it,
                color = if (meeting.isOnlineMeeting) Color(0xFF0E7070) else Color(0xFF2A8B3A),
                fontSize = NexterTypography.Badge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background((if (meeting.isOnlineMeeting) NexterColors.Aqua else NexterColors.Green).copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TasksCard(
    uiState: HomeUiState,
    onAddTask: () -> Unit,
    onToggleCompleted: (HomeTask) -> Unit,
    onDeleteTask: (HomeTask) -> Unit
) {
    HtmlCard(title = "📝", label = stringResource(R.string.home_pending_tasks), action = stringResource(R.string.home_add), onAction = onAddTask) {
        val pendingTasks = uiState.tasks.filter { !it.isCompleted }.take(6)
        if (uiState.isLoading) {
            CardStateMessage(stringResource(R.string.home_loading_tasks), showProgress = true)
        } else if (pendingTasks.isEmpty()) {
            EmptyText(
                text = stringResource(R.string.home_no_pending_tasks),
                subtitle = stringResource(R.string.home_no_pending_tasks_subtitle)
            )
        } else {
            pendingTasks.forEachIndexed { index, task ->
                if (index > 0) Divider(color = NexterColors.border())
                TaskRow(
                    task = task,
                    isCompleted = task.id in uiState.completedTaskIds,
                    onToggleCompleted = { onToggleCompleted(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: HomeTask,
    isCompleted: Boolean,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isCompleted) NexterColors.Green else Color.Transparent)
                .border(BorderStroke(2.dp, if (isCompleted) NexterColors.Green else NexterColors.tertiaryText()), CircleShape)
                .clickable(onClick = onToggleCompleted),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) Text("✓", color = NexterColors.Navy, fontSize = NexterTypography.Caption, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (isCompleted) NexterColors.secondaryText() else NexterColors.primaryText(),
                fontSize = NexterTypography.Callout,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityChip(task.importance)
                task.dueDateText?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Footnote)
                }
            }
        }
        if (isCompleted) {
            Text(
                text = stringResource(R.string.delete),
                color = NexterColors.Red,
                fontSize = NexterTypography.SmallButton,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NexterColors.Red.copy(alpha = 0.10f))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun PriorityChip(importance: String) {
    val color = priorityColor(importance)
    Text(
        text = priorityLabelText(importance),
        color = color,
        fontSize = NexterTypography.Badge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 1.dp)
    )
}

@Composable
private fun HtmlCard(
    title: String,
    label: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        elevation = 0.dp,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NexterColors.border()),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = NexterTypography.CardTitle)
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = NexterColors.primaryText(), fontSize = NexterTypography.CardTitle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                action?.let {
                    Text(
                        it,
                        color = NexterColors.Red,
                        fontSize = NexterTypography.SmallButton,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = onAction != null) { onAction?.invoke() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Divider(color = NexterColors.border())
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp), content = content)
        }
    }
}

@Composable
private fun LoadingCard() {
    HtmlCard(title = "⌛", label = stringResource(R.string.loading)) {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.home_loading_meetings), color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    HtmlCard(title = "!", label = stringResource(R.string.could_not_load)) {
        Text(message, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, modifier = Modifier.padding(top = 10.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.Navy, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(stringResource(R.string.retry), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyText(text: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            color = NexterColors.secondaryText(),
            fontSize = NexterTypography.Callout,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let {
            Text(
                text = it,
                color = NexterColors.tertiaryText(),
                fontSize = NexterTypography.Footnote
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun AgendaDialog(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onScopeSelected: (AgendaScope) -> Unit,
    onRetry: () -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 5.dp
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 5.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = topInset, end = 10.dp, bottom = bottomInset),
            contentAlignment = Alignment.Center
        ) {
            Card(
                backgroundColor = NexterColors.cardBackground(),
                shape = RoundedCornerShape(18.dp),
                elevation = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                AgendaDialogContent(
                    uiState = uiState,
                    onDismiss = onDismiss,
                    onScopeSelected = onScopeSelected,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun AgendaDialogContent(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onScopeSelected: (AgendaScope) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexterColors.cardBackground())
                .padding(start = 18.dp, top = 18.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    stringResource(R.string.agenda_title),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.ScreenTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.agenda_subtitle),
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.ScreenSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                stringResource(R.string.close),
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexterColors.pageBackground())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AgendaScope.values().forEach { scope ->
                    val selected = uiState.agendaScope == scope
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) NexterColors.cardBackground() else Color.Transparent)
                            .clickable { onScopeSelected(scope) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(scope.titleRes),
                            color = if (selected) NexterColors.Red else NexterColors.secondaryText(),
                            fontSize = NexterTypography.SmallButton,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            when {
                uiState.isLoadingAgenda || !uiState.hasLoadedAgenda -> {
                    DialogStateMessage(stringResource(R.string.agenda_loading), showProgress = true)
                }
                uiState.agendaErrorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.agenda_error), color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.Bold)
                        Text(uiState.agendaErrorMessage, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.Red, contentColor = Color.White),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                uiState.visibleAgendaEvents.isEmpty() -> {
                    DialogStateMessage(stringResource(uiState.agendaScope.emptyTitleRes), subtitle = stringResource(R.string.agenda_empty_subtitle))
                }
                else -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        uiState.visibleAgendaEvents.groupedByDay().forEach { group ->
                            AgendaDaySection(group)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaDaySection(group: AgendaEventGroup) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        elevation = 0.dp,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NexterColors.border()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                group.title,
                color = NexterColors.tertiaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 6.dp)
            )
            group.events.forEachIndexed { index, event ->
                if (index > 0) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 86.dp))
                MeetingRow(event, index)
            }
        }
    }
}

@Composable
private fun DialogStateMessage(
    title: String,
    subtitle: String? = null,
    showProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showProgress) CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(24.dp))
        Text(title, color = NexterColors.secondaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(it, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body)
        }
    }
}

@Composable
private fun CardStateMessage(
    title: String,
    showProgress: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showProgress) {
            CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(title, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun AddTaskDialog(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?) -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 5.dp
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 5.dp
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var dueDate by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var showPriorityPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val closeDialog = {
        showPriorityPicker = false
        onDismiss()
    }

    Dialog(
        onDismissRequest = closeDialog,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = topInset, end = 10.dp, bottom = bottomInset),
            contentAlignment = Alignment.Center
        ) {
            Card(
                backgroundColor = NexterColors.cardBackground(),
                shape = RoundedCornerShape(18.dp),
                elevation = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexterColors.cardBackground())
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(R.string.add_task_title),
                                color = NexterColors.primaryText(),
                                fontSize = NexterTypography.ScreenTitle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Divider(color = NexterColors.border())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.title_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(stringResource(R.string.description_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                        )
                        DueDateSelectorRow(
                            dueDateText = dueDateText,
                            onClick = {
                                showDueDatePicker(context) { apiValue, displayValue ->
                                    dueDate = apiValue
                                    dueDateText = displayValue
                                }
                            }
                        )

                        PrioritySelectorRow(
                            priority = priority,
                            onClick = { showPriorityPicker = true }
                        )

                        uiState.createTaskErrorMessage?.let {
                            Text(
                                it,
                                color = NexterColors.Red,
                                fontSize = NexterTypography.Callout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NexterColors.Red.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            )
                        }
                    }

                    Divider(color = NexterColors.border())

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexterColors.cardBackground())
                            .padding(18.dp)
                    ) {
                        OutlinedButton(onClick = closeDialog, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            enabled = title.isNotBlank() && !uiState.isCreatingTask,
                            onClick = { onCreate(title, description, priority, dueDate.ifBlank { null }) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.Red, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isCreatingTask) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPriorityPicker) {
        PriorityPickerDialog(
            selectedPriority = priority,
            onSelected = { selected ->
                priority = selected
                showPriorityPicker = false
            },
            onDismiss = { showPriorityPicker = false }
        )
    }
}

@Composable
private fun DueDateSelectorRow(
    dueDateText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexterColors.pageBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.due_label),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium
            )
            Text(
                dueDateText.ifBlank { stringResource(R.string.no_date) },
                color = if (dueDateText.isBlank()) NexterColors.tertiaryText() else NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text("›", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrioritySelectorRow(
    priority: String,
    onClick: () -> Unit
) {
    val accent = priorityColor(priority)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexterColors.pageBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.priority),
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Footnote,
                fontWeight = FontWeight.Medium
            )
            Text(
                priorityLabelText(priority),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text("›", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriorityPickerDialog(
    selectedPriority: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val priorities = listOf(
        "high" to stringResource(R.string.priority_high),
        "normal" to stringResource(R.string.priority_normal),
        "low" to stringResource(R.string.priority_low)
    )
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
                    stringResource(R.string.priority),
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.CardTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                )
                Divider(color = NexterColors.border())
                priorities.forEachIndexed { index, (value, label) ->
                    val selected = selectedPriority == value
                    val accent = priorityColor(value)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(value) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accent,
                                unselectedColor = NexterColors.tertiaryText()
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            label,
                            color = NexterColors.primaryText(),
                            fontSize = NexterTypography.Body,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index != priorities.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 58.dp))
                }
                Divider(color = NexterColors.border())
                Text(
                    stringResource(R.string.back),
                    color = NexterColors.Red,
                    fontSize = NexterTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
private fun CenteredHomeShell(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

private fun priorityColor(importance: String): Color {
    return when (importance.lowercase(Locale.ROOT)) {
        "high" -> NexterColors.Red
        "low" -> Color(0xFF185FA5)
        else -> Color(0xFFE19A3B)
    }
}

private fun priorityLabel(importance: String): String {
    return when (importance.lowercase(Locale.ROOT)) {
        "high" -> "Alta"
        "low" -> "Baja"
        else -> "Normal"
    }
}

@Composable
private fun priorityLabelText(importance: String): String {
    return when (importance.lowercase(Locale.ROOT)) {
        "high" -> stringResource(R.string.priority_high)
        "low" -> stringResource(R.string.priority_low)
        else -> stringResource(R.string.priority_normal)
    }
}

private fun showDueDatePicker(
    context: Context,
    onSelected: (apiValue: String, displayValue: String) -> Unit
) {
    val initial = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selected.set(Calendar.MINUTE, minute)
                    selected.set(Calendar.SECOND, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    onSelected(formatApiDate(selected), formatDisplayDate(selected))
                },
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatDisplayDate(calendar: Calendar): String {
    return SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
        .format(calendar.time)
        .replace(".", "")
}

private fun formatApiDate(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(calendar.time)
}
