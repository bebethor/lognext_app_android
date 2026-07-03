package com.lognext.nexterandroid.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface APIClient {
    @Throws(APIError::class)
    fun execute(request: Request, acceptedStatusCodes: IntRange = 200..299): String
}

class OkHttpAPIClient(
    private val client: OkHttpClient = OkHttpClient()
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
