package com.lognext.nexterandroid.core.auth

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

data class AuthUser(
    val displayName: String,
    val username: String
)

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val isLoggingIn: StateFlow<Boolean>

    suspend fun restoreSession()
    suspend fun signIn(activity: Activity)
    suspend fun signOut()
    suspend fun requestCalendarWritePermission(activity: Activity) { error("No se puede autorizar la creación de reuniones") }
    suspend fun graphReadToken(scope: String, activity: Activity? = null): String {
        error("Microsoft Graph no está disponible")
    }
    suspend fun currentBffToken(): String?
}

class GraphConsentRequiredException : Exception()
