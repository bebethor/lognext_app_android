package com.lognext.nexterandroid.core.auth

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaceholderAuthRepository : AuthRepository {
    private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = mutableAuthState

    private val mutableIsLoggingIn = MutableStateFlow(false)
    override val isLoggingIn: StateFlow<Boolean> = mutableIsLoggingIn

    override suspend fun restoreSession() {
        mutableAuthState.value = AuthState.Unauthenticated
    }

    override suspend fun signIn(activity: Activity) {
        mutableAuthState.value = AuthState.Authenticated(
            AuthUser(
                displayName = "Usuario",
                username = "Mi SharePoint"
            )
        )
    }

    override suspend fun signOut() {
        mutableAuthState.value = AuthState.Unauthenticated
    }

    override suspend fun currentBffToken(): String? = null
}
