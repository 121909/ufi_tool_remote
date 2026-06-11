package com.example.ufitoolsremote.model

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    val baseUrl: String = "http://192.168.0.1:2333",
    val ufiToken: String = "",
    val adminPassword: String = "",
    val loginModePreference: LoginModePreference = LoginModePreference.LegacyFirst
) {
    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')
}

@Serializable
enum class LoginModePreference {
    MultiUserFirst,
    LegacyFirst
}
