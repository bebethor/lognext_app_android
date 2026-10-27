package com.lognext.nexterandroid.core.network

import com.lognext.nexterandroid.core.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlinx.coroutines.CancellationException

interface APIClient {
    @Throws(APIError::class)
    fun execute(request: Request, acceptedStatusCodes: IntRange = 200..299): String
}

class OkHttpAPIClient(
    private val authRepository: AuthRepository? = null,
    private val cache: ResponseCache? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (authRepository != null) {
                addInterceptor(BffAuthInterceptor(authRepository))
            }
        }
        .build()
) : APIClient {
    private val inFlight = SingleFlight<String>()

    override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
        val account = (authRepository?.authState?.value as? com.lognext.nexterandroid.core.auth.AuthState.Authenticated)?.user?.username
        val cacheable = request.method == "GET" && account != null &&
            request.header("Cache-Control") != "no-store" && request.url.encodedPath.startsWith("/api/v1/")
        if (cacheable) {
            return inFlight.execute("$account|${request.url}|$acceptedStatusCodes") {
                // Re-check after becoming the owner: another request may have filled the cache.
                cache?.read(account!!, request.url.toString())
                    ?: fetch(request, acceptedStatusCodes, account, true)
            }
        }
        return fetch(request, acceptedStatusCodes, account, false)
    }

    private fun fetch(request: Request, acceptedStatusCodes: IntRange, account: String?, cacheable: Boolean): String {
        try {
            client.newCall(request).execute().use { response ->
                if (response.code !in acceptedStatusCodes) {
                    throw APIError.Http(response.code, response.serverMessage())
                }
                val body = response.body?.string() ?: throw APIError.InvalidResponse
                val currentAccount = (authRepository?.authState?.value as? com.lognext.nexterandroid.core.auth.AuthState.Authenticated)?.user?.username
                if (cacheable && account == currentAccount) {
                    runCatching { cache?.write(account!!, request.url.toString(), body) }
                    cache?.markStale(request.url.toString(), false)
                } else if (account != null && request.method != "GET") {
                    cache?.clear(account)
                }
                return body
            }
        } catch (error: IOException) {
            val currentAccount = (authRepository?.authState?.value as? com.lognext.nexterandroid.core.auth.AuthState.Authenticated)?.user?.username
            if (cacheable && account == currentAccount) {
                cache?.read(account!!, request.url.toString(), maxAge = null)?.let {
                    cache.markStale(request.url.toString(), true)
                    return it
                }
            }
            throw error
        }
    }

    private fun Response.serverMessage(): String {
        return body?.string().orEmpty()
    }
}

internal class BffAuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = try {
            runBlocking { authRepository.currentBffToken() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw IOException("No se pudo obtener acceso a la API. Vuelve a iniciar sesión.", error)
        }
        if (token.isNullOrBlank()) {
            throw IOException("No hay una sesión válida para la API. Vuelve a iniciar sesión.")
        }
        val authorizedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authorizedRequest)
    }
}
