package com.lognext.nexterandroid.core.network

import com.google.gson.Gson
import com.lognext.nexterandroid.core.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

open class RestService(
    private val apiClient: APIClient,
    private val gson: Gson = Gson()
) {
    protected suspend fun <T> get(path: String, type: Class<T>, query: Map<String, String?> = emptyMap()): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url(path, query))
                .header("Accept", "application/json")
                .get()
                .build()

            gson.fromJson(apiClient.execute(request), type)
        }
    }

    protected suspend fun <T> post(path: String, body: Any?, type: Class<T>, acceptedStatusCodes: IntRange = 200..299): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url(path))
                .header("Accept", "application/json")
                .header("Content-Type", JsonContentType)
                .post(jsonBody(body))
                .build()

            gson.fromJson(apiClient.execute(request, acceptedStatusCodes), type)
        }
    }

    protected suspend fun patch(path: String, body: Any?, acceptedStatusCodes: IntRange = 200..299) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url(path))
                .header("Accept", "application/json")
                .header("Content-Type", JsonContentType)
                .patch(jsonBody(body))
                .build()

            apiClient.execute(request, acceptedStatusCodes)
        }
    }

    protected suspend fun delete(path: String, acceptedStatusCodes: IntRange = 200..299) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url(path))
                .header("Accept", "application/json")
                .delete()
                .build()

            apiClient.execute(request, acceptedStatusCodes)
        }
    }

    protected suspend fun <T> delete(path: String, type: Class<T>, acceptedStatusCodes: IntRange = 200..299): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url(path))
                .header("Accept", "application/json")
                .delete()
                .build()

            gson.fromJson(apiClient.execute(request, acceptedStatusCodes), type)
        }
    }

    private fun url(path: String, query: Map<String, String?> = emptyMap()): String {
        val builder = AppConfig.BaseUrl.trimEnd('/').toHttpUrl().newBuilder()
        path.trimStart('/').split('/').filter { it.isNotBlank() }.forEach(builder::addPathSegment)
        query.forEach { (name, value) ->
            if (!value.isNullOrBlank()) builder.addQueryParameter(name, value)
        }
        return builder.build().toString()
    }

    private fun jsonBody(body: Any?) = gson.toJson(body ?: emptyMap<String, Any>())
        .toRequestBody(JsonContentType.toMediaType())

    private companion object {
        const val JsonContentType = "application/json; charset=utf-8"
    }
}
