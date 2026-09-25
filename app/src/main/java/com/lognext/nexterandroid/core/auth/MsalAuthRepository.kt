package com.lognext.nexterandroid.core.auth

import android.app.Activity
import android.content.Context
import com.lognext.nexterandroid.core.AppConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            val currentAccount = withContext(Dispatchers.IO) {
                app.currentAccount.currentAccount
            }
            account = currentAccount
            if (currentAccount != null) acquireBffToken(app, currentAccount)
            mutableAuthState.value = currentAccount?.toAuthState() ?: AuthState.Unauthenticated
        }.onFailure { error ->
            if (error is CancellationException) throw error
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
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mutableAuthState.value = AuthState.Error(error.authMessage())
        } finally {
            mutableIsLoggingIn.value = false
        }
    }

    override suspend fun signOut() {
        if (AppConfig.UseFakeLogin) {
            account = null
            mutableAuthState.value = AuthState.Unauthenticated
            return
        }

        runCatching {
            withContext(Dispatchers.IO) {
                getApplication().signOut()
            }
            account = null
            mutableAuthState.value = AuthState.Unauthenticated
        }.onFailure { error ->
            if (error is CancellationException) throw error
            mutableAuthState.value = AuthState.Error(error.authMessage())
        }
    }

    override suspend fun currentBffToken(): String? {
        if (AppConfig.UseFakeLogin) return null

        val resolvedAccount = account ?: error("La sesión no está disponible. Vuelve a iniciar sesión.")
        // MSAL manages token expiry and refresh; do not retain an access token indefinitely.
        return acquireBffToken(getApplication(), resolvedAccount)
    }

    private suspend fun getApplication(): ISingleAccountPublicClientApplication {
        application?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                MsalSigningConfiguration.resourceForInstalledApp(context),
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        this@MsalAuthRepository.application = application
                        if (continuation.isActive) continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
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

        return withContext(Dispatchers.IO) {
            app.acquireTokenSilent(
                AppConfig.bffScopes.toTypedArray(),
                account.authority
            ).accessToken
        }
    }

    private suspend fun ISingleAccountPublicClientApplication.signInAwait(
        activity: Activity,
        scopes: List<String>
    ): IAuthenticationResult {
        return suspendCancellableCoroutine { continuation ->
            val builder = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(scopes)

            val parameters = builder
                .withCallback(
                    object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            if (continuation.isActive) continuation.resume(authenticationResult)
                        }

                        override fun onError(exception: MsalException) {
                            if (continuation.isActive) continuation.resumeWithException(exception)
                        }

                        override fun onCancel() {
                            if (continuation.isActive) continuation.resumeWithException(AuthCancelledException())
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
