package com.example.ufitoolsremote.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppSettings(
    val connection: ConnectionConfig = ConnectionConfig(),
    val widgetRefreshMinutes: Int = 15,
    val quickReplies: List<QuickReplyPreset> = emptyList(),
    val easyTier: EasyTierSettings = EasyTierSettings()
)

@Serializable
data class EasyTierSettings(
    val enabled: Boolean = false,
    val instanceName: String = "ufi-remote",
    val networkName: String = "",
    val networkSecret: String = "",
    val hostname: String = "",
    val virtualIpv4: String = "",
    val peers: List<String> = emptyList(),
    val listeners: List<String> = emptyList(),
    val proxyNetworks: List<String> = emptyList(),
    val socks5Enabled: Boolean = true,
    val socks5Host: String = "0.0.0.0",
    val socks5Port: Int = 1080,
    val logLevel: String = "info"
)

fun AppSettings.resolvedConnectionConfig(): ConnectionConfig {
    if (connection.accessMode != UfiAccessMode.EasyTierSocks5) return connection

    val resolvedHost = when (val bindHost = easyTier.socks5Host.trim()) {
        "", "0.0.0.0", "::", "[::]" -> "127.0.0.1"
        else -> bindHost
    }

    return connection.copy(
        easyTierSocks5Host = resolvedHost,
        easyTierSocks5Port = easyTier.socks5Port
    )
}

@Serializable
data class QuickReplyPreset(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val number: String = "",
    val message: String = "",
    val sendMode: QuickReplySendMode = QuickReplySendMode.Confirm
)

@Serializable
enum class QuickReplySendMode {
    Direct,
    Confirm
}

@Serializable
data class SmsCacheSnapshot(
    val messages: List<SmsMessage> = emptyList(),
    val updatedAtMillis: Long = 0L,
    val statusText: String = "",
    val statusAtMillis: Long = 0L
)
