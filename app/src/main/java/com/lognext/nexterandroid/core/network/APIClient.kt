package com.lognext.nexterandroid.core.network

import com.lognext.nexterandroid.core.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface APIClient {
    @Throws(APIError::class)
    fun execute(request: Request, acceptedStatusCodes: IntRange = 200..299): String
}

class OkHttpAPIClient(
    authRepository: AuthRepository? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (authRepository != null) {
                addInterceptor(BffAuthInterceptor(authRepository))
            }
        }
        .build()
) : APIClient {
    override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
        client.newCall(request).execute().use { response ->
            if (response.code !in acceptedStatusCodes) {
                throw APIError.Http(response.code, response.serverMessage())
            }

            return response.body?.string() ?: throw APIError.InvalidResponse
        }
    }

    private fun Response.serverMessage(): String {
        return body?.string().orEmpty()
    }
}

private class BffAuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { authRepository.currentBffToken() }
        val authorizedRequest = if (token.isNullOrBlank()) {
            originalRequest
        } else {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(authorizedRequest)
    }
}
