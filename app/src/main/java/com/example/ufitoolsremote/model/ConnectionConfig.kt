package com.example.ufitoolsremote.model

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    val baseUrl: String = "http://192.168.0.1:2333",
    val ufiToken: String = "",
    val adminPassword: String = "",
    val loginModePreference: LoginModePreference = LoginModePreference.LegacyFirst,
    val accessMode: UfiAccessMode = UfiAccessMode.Direct,
    val easyTierSocks5Host: String = "127.0.0.1",
    val easyTierSocks5Port: Int = 1080
) {
    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    val normalizedEasyTierSocks5Host: String
        get() = easyTierSocks5Host.trim()
}

@Serializable
enum class LoginModePreference {
    MultiUserFirst,
    LegacyFirst
}

@Serializable
enum class UfiAccessMode {
    Direct,
    EasyTierSocks5
}
