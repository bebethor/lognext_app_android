package com.lognext.nexterandroid.core.network

sealed class APIError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    object InvalidResponse : APIError("Respuesta inválida del servidor.")
    data class Http(val statusCode: Int, val serverMessage: String) : APIError(
        if (serverMessage.isBlank()) "Error del servidor. Código: $statusCode." else serverMessage
    )
    class Decoding(cause: Throwable) : APIError("No se pudo interpretar la respuesta del servidor.", cause)
}
