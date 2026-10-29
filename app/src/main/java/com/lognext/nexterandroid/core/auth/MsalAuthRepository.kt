package com.lognext.nexterandroid.core.auth

import android.app.Activity
import android.content.Context
import com.lognext.nexterandroid.core.AppConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MsalAuthRepository(
    private val context: Context
) : AuthRepository {
    private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = mutableAuthState

    private val mutableIsLoggingIn = MutableStateFlow(false)
    override val isLoggingIn: StateFlow<Boolean> = mutableIsLoggingIn

    private var application: ISingleAccountPublicClientApplication? = null
    private var account: IAccount? = null

    override suspend fun restoreSession() {
        if (AppConfig.UseFakeLogin) {
            mutableAuthState.value = AuthState.Unauthenticated
            return
        }

        mutableAuthState.value = AuthState.Loading

        runCatching {
            val app = getApplication()
            val currentAccount = awaitAuthCallback<IAccount?> { complete ->
                app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                    override fun onAccountLoaded(activeAccount: IAccount?) { complete(Result.success(activeAccount)) }
                    override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) = Unit
                    override fun onError(exception: MsalException) { complete(Result.failure(exception)) }
                })
            }
            account = currentAccount
            if (currentAccount != null) acquireBffToken(app, currentAccount)
            mutableAuthState.value = currentAccount?.toAuthState() ?: AuthState.Unauthenticated
        }.onFailure { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) {
                if (mutableAuthState.value == AuthState.Loading) mutableAuthState.value = AuthState.Unauthenticated
                throw error
            }
            mutableAuthState.value = AuthState.Error(error.authMessage())
        }
    }

    override suspend fun signIn(activity: Activity) {
        if (mutableIsLoggingIn.value) return

        mutableIsLoggingIn.value = true
        try {
            if (AppConfig.UseFakeLogin) {
                mutableAuthState.value = AuthState.Authenticated(
                    AuthUser(
                        displayName = "Jose Alberto",
                        username = "jose.alberto@lognext.com"
                    )
                )
                return
            }

            val app = getApplication()
            require(AppConfig.bffScopes.isNotEmpty()) { "No está configurado el acceso a la API de Lognext." }
            val result = app.signInAwait(activity, AppConfig.bffScopes)
            check(result.accessToken.isNotBlank()) { "Microsoft no ha devuelto un token de acceso a la API." }
            account = result.account
            mutableAuthState.value = result.account.toAuthState()
        } catch (error: TimeoutCancellationException) {
            mutableAuthState.value = AuthState.Error(error.authMessage())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mutableAuthState.value = AuthState.Error(error.authMessage())
        } finally {
            mutableIsLoggingIn.value = false
        }
    }

    override suspend fun signOut() {
        com.lognext.nexterandroid.core.AppDependencies.absenceNotifications.beforeSignOut()
        com.lognext.nexterandroid.core.AppDependencies.clearResponseCache(delete = true)
        if (AppConfig.UseFakeLogin) {
            account = null
            mutableAuthState.value = AuthState.Unauthenticated
            return
        }

        runCatching {
            val app = getApplication()
            awaitAuthCallback<Unit> { complete ->
                app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() { complete(Result.success(Unit)) }
                    override fun onError(exception: MsalException) { complete(Result.failure(exception)) }
                })
            }
            account = null
            mutableAuthState.value = AuthState.Unauthenticated
        }.onFailure { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) {
                if (mutableAuthState.value == AuthState.Loading) mutableAuthState.value = AuthState.Unauthenticated
                throw error
            }
            mutableAuthState.value = AuthState.Error(error.authMessage())
        }
    }

    override suspend fun currentBffToken(): String? {
        if (AppConfig.UseFakeLogin) return null

        val resolvedAccount = account ?: error("La sesión no está disponible. Vuelve a iniciar sesión.")
        // MSAL manages token expiry and refresh; do not retain an access token indefinitely.
        return acquireBffToken(getApplication(), resolvedAccount)
    }

    override suspend fun requestCalendarWritePermission(activity: Activity) {
        if (AppConfig.UseFakeLogin) return
        val app = getApplication()
        val resolvedAccount = account ?: error("Vuelve a iniciar sesión")
        try {
            app.silentTokenAwait(listOf("Calendars.ReadWrite"), resolvedAccount)
        } catch (error: com.microsoft.identity.client.exception.MsalUiRequiredException) {
            val result = app.signInAwait(activity, listOf("Calendars.ReadWrite"), resolvedAccount)
            check(result.account.username.equals(resolvedAccount.username, ignoreCase = true)) {
                "Debes autorizar con la misma cuenta con la que has iniciado sesión."
            }
        }
    }

    override suspend fun graphReadToken(scope: String, activity: Activity?): String {
        require(scope in setOf("Calendars.Read", "Place.Read.All"))
        val app = getApplication()
        val resolvedAccount = account ?: error("Vuelve a iniciar sesión")
        return try {
            app.silentTokenAwait(listOf(scope), resolvedAccount).accessToken
        } catch (required: com.microsoft.identity.client.exception.MsalUiRequiredException) {
            if (activity == null) throw GraphConsentRequiredException()
            val result = app.signInAwait(activity, listOf(scope), resolvedAccount)
            check(result.account.username.equals(resolvedAccount.username, ignoreCase = true)) {
                "Debes autorizar con la misma cuenta con la que has iniciado sesión."
            }
            result.accessToken
        }
    }

    private suspend fun getApplication(): ISingleAccountPublicClientApplication {
        application?.let { return it }

        return awaitAuthCallback { complete ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                MsalSigningConfiguration.resourceForInstalledApp(context),
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        this@MsalAuthRepository.application = application
                        complete(Result.success(application))
                    }

                    override fun onError(exception: MsalException) {
                        complete(Result.failure(exception))
                    }
                }
            )
        }
    }

    private suspend fun acquireBffToken(
        app: ISingleAccountPublicClientApplication,
        account: IAccount
    ): String {
        require(AppConfig.bffScopes.isNotEmpty()) { "No está configurado el acceso a la API de Lognext." }

        return app.silentTokenAwait(AppConfig.bffScopes, account).accessToken
    }

    private suspend fun ISingleAccountPublicClientApplication.silentTokenAwait(
        scopes: List<String>, account: IAccount
    ): IAuthenticationResult = awaitAuthCallback { complete ->
        acquireTokenSilentAsync(scopes.toTypedArray(), account.authority, object : SilentAuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) { complete(Result.success(result)) }
            override fun onError(exception: MsalException) { complete(Result.failure(exception)) }
        })
    }

    private suspend fun ISingleAccountPublicClientApplication.signInAwait(
        activity: Activity,
        scopes: List<String>,
        permissionAccount: IAccount? = null
    ): IAuthenticationResult {
        return awaitAuthCallback(timeoutMillis = 180_000) { complete ->
            val builder = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(scopes)
            account?.let { builder.withLoginHint(it.username) }
            permissionAccount?.let {
                // Additional permissions belong to the existing session. Let Microsoft
                // reuse its session instead of asking the user to select an account again.
                builder.forAccount(it)
                    .fromAuthority(it.authority)
                    .withPrompt(Prompt.WHEN_REQUIRED)
            }

            val parameters = builder
                .withCallback(
                    object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            complete(Result.success(authenticationResult))
                        }

                        override fun onError(exception: MsalException) {
                            complete(Result.failure(exception))
                        }

                        override fun onCancel() {
                            complete(Result.failure(AuthCancelledException()))
                        }
                    }
                )
                .build()

            acquireToken(parameters)
        }
    }

    private fun IAccount.toAuthState(): AuthState.Authenticated {
        val displayName = claims?.get("name") as? String ?: username
        return AuthState.Authenticated(
            AuthUser(
                displayName = displayName,
                username = username
            )
        )
    }

    private fun Throwable.authMessage(): String {
        return when (this) {
            is TimeoutCancellationException -> "Microsoft está tardando demasiado en responder. Comprueba la conexión y vuelve a intentarlo."
            is AuthCancelledException -> "Inicio de sesión cancelado."
            is MsalException -> listOfNotNull(
                "No se pudo completar la autenticación.",
                errorCode.takeIf { it.isNotBlank() }?.let { "Código: $it" },
                message?.takeIf { it.isNotBlank() }
            ).joinToString("\n")
            else -> message ?: "No se pudo completar la autenticación."
        }
    }
}

private class AuthCancelledException : Exception()
