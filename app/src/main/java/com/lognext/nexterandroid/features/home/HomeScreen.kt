package com.lognext.nexterandroid.features.home

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Nexter", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(12.dp))

        when (val state = authState) {
            AuthState.Loading -> {
                CircularProgressIndicator()
            }
            AuthState.Unauthenticated -> {
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
                Text(
                    text = "Hola, ${state.user.displayName}",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = state.user.username,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch { authRepository.signOut() }
                    }
                ) {
                    Text("Cerrar sesion")
                }
            }
            is AuthState.Error -> {
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
}
