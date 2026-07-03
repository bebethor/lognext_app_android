package com.lognext.nexterandroid.core.auth

data class AuthUser(
    val displayName: String,
    val username: String
)

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
}

interface AuthRepository {
    val authState: AuthState
    val isLoggingIn: Boolean

    suspend fun restoreSession()
    suspend fun signIn()
    suspend fun signOut()
    suspend fun currentBffToken(): String?
}
