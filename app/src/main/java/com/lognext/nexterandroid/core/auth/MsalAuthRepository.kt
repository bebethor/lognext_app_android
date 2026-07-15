package com.lognext.nexterandroid.core.auth

import android.app.Activity
import android.content.Context
import com.lognext.nexterandroid.R
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
    private var bffAccessToken: String? = null

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
            bffAccessToken = null
            mutableAuthState.value = currentAccount?.toAuthState() ?: AuthState.Unauthenticated
        }.onFailure { error ->
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
            val result = app.signInAwait(activity, AppConfig.graphScopes)
            account = result.account
            bffAccessToken = runCatching {
                acquireBffToken(app, result.account)
            }.getOrNull()
            mutableAuthState.value = result.account.toAuthState()
        } catch (error: Throwable) {
            mutableAuthState.value = AuthState.Error(error.authMessage())
        } finally {
            mutableIsLoggingIn.value = false
        }
    }

    override suspend fun signOut() {
        if (AppConfig.UseFakeLogin) {
            account = null
            bffAccessToken = null
            mutableAuthState.value = AuthState.Unauthenticated
            return
        }

        runCatching {
            withContext(Dispatchers.IO) {
                getApplication().signOut()
            }
            account = null
            bffAccessToken = null
            mutableAuthState.value = AuthState.Unauthenticated
        }.onFailure { error ->
            mutableAuthState.value = AuthState.Error(error.authMessage())
        }
    }

    override suspend fun currentBffToken(): String? {
        if (AppConfig.UseFakeLogin) return null

        bffAccessToken?.let { return it }

        val resolvedAccount = account ?: return null
        return runCatching {
            acquireBffToken(getApplication(), resolvedAccount)
        }.onSuccess { token ->
            bffAccessToken = token
        }.getOrNull()
    }

    private suspend fun getApplication(): ISingleAccountPublicClientApplication {
        application?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_auth_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        this@MsalAuthRepository.application = application
                        continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private fun acquireBffToken(
        app: ISingleAccountPublicClientApplication,
        account: IAccount
    ): String? {
        if (AppConfig.bffScopes.isEmpty()) return null

        val result = app.acquireTokenSilent(
            AppConfig.bffScopes.toTypedArray(),
            account.authority
        )
        return result.accessToken
    }

    private suspend fun ISingleAccountPublicClientApplication.signInAwait(
        activity: Activity,
        scopes: List<String>
    ): IAuthenticationResult {
        return suspendCancellableCoroutine { continuation ->
            val builder = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(scopes)

            if (AppConfig.bffScopes.isNotEmpty()) {
                builder.withOtherScopesToAuthorize(AppConfig.bffScopes)
            }

            val parameters = builder
                .withCallback(
                    object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            continuation.resume(authenticationResult)
                        }

                        override fun onError(exception: MsalException) {
                            continuation.resumeWithException(exception)
                        }

                        override fun onCancel() {
                            continuation.resumeWithException(AuthCancelledException())
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
