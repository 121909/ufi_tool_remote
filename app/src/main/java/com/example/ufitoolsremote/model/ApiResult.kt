package com.example.ufitoolsremote.model

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data object Unauthorized : ApiResult<Nothing>
    data class NetworkError(val message: String) : ApiResult<Nothing>
    data class ParseError(val message: String) : ApiResult<Nothing>
    data class DeviceError(val message: String) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Unauthorized -> this
    is ApiResult.NetworkError -> this
    is ApiResult.ParseError -> this
    is ApiResult.DeviceError -> this
}

fun ApiResult<*>.messageOrNull(): String? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.Unauthorized -> "UFI-TOOLS 口令无效或已过期"
    is ApiResult.NetworkError -> message
    is ApiResult.ParseError -> message
    is ApiResult.DeviceError -> message
}
