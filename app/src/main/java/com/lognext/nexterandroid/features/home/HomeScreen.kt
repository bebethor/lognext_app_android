package com.lognext.nexterandroid.features.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.AuthState
import kotlinx.coroutines.launch

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
            CircularProgressIndicator()
        }
        AuthState.Unauthenticated -> CenteredHomeShell {
            Text(text = "Nexter", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Inicia sesion con Microsoft para acceder a LogNext.",
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                enabled = !isLoggingIn && activity != null,
                onClick = {
                    activity?.let {
                        scope.launch { authRepository.signIn(it) }
                    }
                }
            ) {
                Text(if (isLoggingIn) "Conectando..." else "Entrar con Microsoft")
            }
        }
        is AuthState.Authenticated -> {
            AuthenticatedHome(
                authState = state,
                onSignOut = { scope.launch { authRepository.signOut() } }
            )
        }
        is AuthState.Error -> CenteredHomeShell {
            Text(text = "Nexter", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.message,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                enabled = !isLoggingIn && activity != null,
                onClick = {
                    activity?.let {
                        scope.launch { authRepository.signIn(it) }
                    }
                }
            ) {
                Text("Reintentar")
            }
        }
    }
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
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Nexter", style = MaterialTheme.typography.h5)
                Text(
                    text = "Hola, ${uiState.firstName.ifBlank { authState.user.displayName }}",
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.72f)
                )
            }
            OutlinedButton(onClick = onSignOut) {
                Text("Salir")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (uiState.isLoading) {
            LinearLoadingCard()
        }

        uiState.errorMessage?.let {
            ErrorCard(message = it, onRetry = viewModel::refresh)
            Spacer(modifier = Modifier.height(14.dp))
        }

        SummaryGrid(uiState)
        Spacer(modifier = Modifier.height(14.dp))
        MeetingsCard(uiState)
        Spacer(modifier = Modifier.height(14.dp))
        TasksCard(uiState)
    }
}

@Composable
private fun CenteredHomeShell(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun LinearLoadingCard() {
    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.width(28.dp).height(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Cargando tu inicio...")
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message, color = MaterialTheme.colors.error)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun SummaryGrid(uiState: HomeUiState) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Vacaciones", uiState.formattedVacationDays, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            SummaryTile("Reuniones", (uiState.meetingsTodayCount ?: uiState.todayMeetings.size).toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Tareas", uiState.displayedPendingTasksCount.toString(), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            SummaryTile("Urgentes", (uiState.urgentTasksCount ?: uiState.tasks.count { it.importance == "high" }).toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MeetingsCard(uiState: HomeUiState) {
    HomeCard(title = "Reuniones de hoy") {
        when {
            uiState.visibleMeetings.isEmpty() && !uiState.isLoading -> {
                Text("No tienes reuniones pendientes hoy.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            }
            else -> {
                uiState.visibleMeetings.forEachIndexed { index, meeting ->
                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 10.dp))
                    Row {
                        Text(meeting.startTimeText, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meeting.subject, style = MaterialTheme.typography.subtitle2)
                            Text(meeting.subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.66f))
                        }
                        meeting.tagText?.let {
                            Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksCard(uiState: HomeUiState) {
    HomeCard(title = "Tareas") {
        val pendingTasks = uiState.tasks.filter { !it.isCompleted }.take(4)
        if (pendingTasks.isEmpty() && !uiState.isLoading) {
            Text("No tienes tareas pendientes.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
        } else {
            pendingTasks.forEachIndexed { index, task ->
                if (index > 0) Divider(modifier = Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .width(8.dp)
                            .height(8.dp)
                            .background(priorityColor(task.importance), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.subtitle2)
                        Text(
                            text = listOfNotNull(task.priorityLabel, task.dueDateText).joinToString(" · "),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

private fun priorityColor(importance: String): Color {
    return when (importance.lowercase()) {
        "high" -> Color(0xFFE25555)
        "low" -> Color(0xFF3D7BE0)
        else -> Color(0xFFE19A3B)
    }
}
