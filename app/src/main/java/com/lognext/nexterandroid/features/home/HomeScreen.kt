package com.lognext.nexterandroid.features.home

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                authState = state,
                onSignOut = { scope.launch { authRepository.signOut() } }
            )
        }
        is AuthState.Error -> LoginScreen(
            isSigningIn = isLoggingIn,
            errorMessage = null,
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
    val isDark = isSystemInDarkTheme()
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
        if (!isDark) {
            Image(
                painter = painterResource(id = R.drawable.login_bottom_wave),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 100.dp)
            )
        }

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
                        text = "INICIAR SESIÓN",
                        color = NexterColors.primaryText(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = NexterColors.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            Column(
                modifier = Modifier.alpha(textAlpha),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LoginClaimLine(red = "Tecnología ", navy = "que construye.")
                LoginClaimLine(red = "Compromiso ", navy = "que perdura.")
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
        fontSize = 22.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun AuthenticatedHome(
    authState: AuthState.Authenticated,
    onSignOut: () -> Unit
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

    LaunchedEffect(authState.user.username) {
        viewModel.loadIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
    ) {
        TopBar(authState.user.displayName, onSignOut)

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
            MeetingsCard(uiState)
            TasksCard(uiState)
        }
    }
}

@Composable
private fun TopBar(displayName: String, onSignOut: () -> Unit) {
    val date = remember {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date()).replace(".", "")
    }
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.lognext_logo_negative else R.drawable.lognext_logo
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) NexterColors.cardBackground() else Color.Transparent)
            .height(if (isDark) 70.dp else 54.dp)
            .padding(horizontal = 24.dp, vertical = if (isDark) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "Lognext",
            modifier = Modifier
                .height(30.dp)
                .width(132.dp)
                .weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = date, color = if (isDark) Color.White.copy(alpha = 0.72f) else NexterColors.Navy.copy(alpha = 0.35f), fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NexterColors.Red),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials(displayName), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, NexterColors.Red.copy(alpha = 0.7f)),
            modifier = Modifier.size(35.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logout),
                contentDescription = "Cerrar sesión",
                tint = NexterColors.Red,
                modifier = Modifier.size(20.dp)
            )
        }
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
            Text("Bienvenido de nuevo,", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Text(
                text = uiState.firstName.ifBlank { displayName },
                color = Color.White,
                fontSize = 20.sp,
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
                Text("ACTIVO", color = Color.White.copy(alpha = 0.80f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                GreetingStat(uiState.formattedVacationDays, "Dias dispon.", Modifier.weight(1f))
                GreetingDivider()
                GreetingStat((uiState.meetingsTodayCount ?: uiState.todayMeetings.size).toString(), "Reuniones hoy", Modifier.weight(1f))
                GreetingDivider()
                GreetingStat(uiState.displayedPendingTasksCount.toString(), "Tareas pend.", Modifier.weight(1f))
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun GreetingStat(number: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
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
private fun MeetingsCard(uiState: HomeUiState) {
    HtmlCard(title = "📅", label = "Reuniones de hoy", action = "Ver agenda →") {
        when {
            uiState.visibleMeetings.isEmpty() && !uiState.isLoading -> EmptyText("No tienes reuniones hoy")
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
            fontSize = 11.sp,
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
            Text(meeting.subject, color = NexterColors.primaryText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(meeting.subtitle, color = NexterColors.tertiaryText(), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        meeting.tagText?.let {
            Text(
                text = it,
                color = if (meeting.isOnlineMeeting) Color(0xFF0E7070) else Color(0xFF2A8B3A),
                fontSize = 9.sp,
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
private fun TasksCard(uiState: HomeUiState) {
    HtmlCard(title = "📝", label = "Tareas pendientes", action = "Añadir") {
        val pendingTasks = uiState.tasks.filter { !it.isCompleted }.take(6)
        if (pendingTasks.isEmpty() && !uiState.isLoading) {
            EmptyText("No tienes tareas pendientes")
        } else {
            pendingTasks.forEachIndexed { index, task ->
                if (index > 0) Divider(color = NexterColors.border())
                TaskRow(task)
            }
        }
    }
}

@Composable
private fun TaskRow(task: HomeTask) {
    Row(modifier = Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, NexterColors.tertiaryText()), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = NexterColors.primaryText(), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityChip(task.importance, task.priorityLabel)
                task.dueDateText?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it, color = NexterColors.tertiaryText(), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PriorityChip(importance: String, label: String) {
    val color = priorityColor(importance)
    Text(
        text = label,
        color = color,
        fontSize = 9.sp,
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
                Text(title, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = NexterColors.primaryText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                action?.let {
                    Text(it, color = NexterColors.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Divider(color = NexterColors.border())
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp), content = content)
        }
    }
}

@Composable
private fun LoadingCard() {
    HtmlCard(title = "⌛", label = "Cargando") {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = NexterColors.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Cargando tu inicio...", color = NexterColors.secondaryText(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    HtmlCard(title = "!", label = "No se pudo cargar") {
        Text(message, color = NexterColors.secondaryText(), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(backgroundColor = NexterColors.Navy, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text("Reintentar", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = NexterColors.secondaryText(),
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 20.dp)
    )
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

private fun initials(displayName: String): String {
    return displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "JA" }
}
