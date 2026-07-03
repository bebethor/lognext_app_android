package com.lognext.nexterandroid.core.auth

class PlaceholderAuthRepository : AuthRepository {
    override var authState: AuthState = AuthState.Unauthenticated
        private set

    override var isLoggingIn: Boolean = false
        private set

    override suspend fun restoreSession() {
        authState = AuthState.Unauthenticated
    }

    override suspend fun signIn() {
        authState = AuthState.Authenticated(
            AuthUser(
                displayName = "Usuario",
                username = "Mi SharePoint"
            )
        )
    }

    override suspend fun signOut() {
        authState = AuthState.Unauthenticated
    }

    override suspend fun currentBffToken(): String? = null
}
