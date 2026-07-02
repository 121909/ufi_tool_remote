package com.example.ufitoolsremote.data

import android.content.Context
import com.example.ufitoolsremote.model.AppSettings
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.EasyTierSettings
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun current(): AppSettings = _settings.value

    fun update(transform: AppSettings.() -> AppSettings): AppSettings = synchronized(lock) {
        val updated = _settings.value.transform().normalize()
        persist(updated)
        _settings.value = updated
        updated
    }

    fun replace(settings: AppSettings): AppSettings = synchronized(lock) {
        val updated = settings.normalize()
        persist(updated)
        _settings.value = updated
        updated
    }

    fun updateConnection(transform: ConnectionConfig.() -> ConnectionConfig): AppSettings {
        return update { copy(connection = connection.transform()) }
    }

    fun updateEasyTier(transform: EasyTierSettings.() -> EasyTierSettings): AppSettings {
        return update { copy(easyTier = easyTier.transform()) }
    }

    fun updateWidgetRefreshMinutes(minutes: Int): AppSettings {
        return update { copy(widgetRefreshMinutes = minutes.coerceIn(MIN_REFRESH_MINUTES, MAX_REFRESH_MINUTES)) }
    }

    fun upsertQuickReply(preset: QuickReplyPreset): AppSettings {
        return update {
            val updated = quickReplies.toMutableList()
            val index = updated.indexOfFirst { it.id == preset.id }
            if (index >= 0) {
                updated[index] = preset
            } else {
                updated.add(0, preset)
            }
            copy(quickReplies = updated)
        }
    }

    fun deleteQuickReply(id: String): AppSettings {
        return update { copy(quickReplies = quickReplies.filterNot { it.id == id }) }
    }

    fun addQuickReply(): QuickReplyPreset {
        return QuickReplyPreset(
            label = "快捷回复",
            number = "",
            message = "",
            sendMode = QuickReplySendMode.Confirm
        )
    }

    fun currentConnection(): ConnectionConfig = current().connection

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS_JSON, null).orEmpty()
        if (raw.isBlank()) return AppSettings()
        return runCatching { json.decodeFromString(AppSettings.serializer(), raw).normalize() }.getOrDefault(AppSettings())
    }

    private fun persist(settings: AppSettings) {
        prefs.edit().putString(KEY_SETTINGS_JSON, json.encodeToString(AppSettings.serializer(), settings)).apply()
    }

    private fun AppSettings.normalize(): AppSettings {
        return copy(
            connection = connection.normalize(),
            widgetRefreshMinutes = widgetRefreshMinutes.coerceIn(MIN_REFRESH_MINUTES, MAX_REFRESH_MINUTES),
            quickReplies = quickReplies.map { it.normalize() },
            easyTier = easyTier.normalize()
        )
    }

    private fun ConnectionConfig.normalize(): ConnectionConfig {
        return copy(
            baseUrl = baseUrl.trim(),
            ufiToken = ufiToken.trim(),
            adminPassword = adminPassword.trim(),
            easyTierSocks5Host = easyTierSocks5Host.trim().ifBlank { "127.0.0.1" },
            easyTierSocks5Port = easyTierSocks5Port.coerceIn(1, 65535)
        )
    }

    private fun QuickReplyPreset.normalize(): QuickReplyPreset {
        return copy(
            label = label.trim(),
            number = number.trim(),
            message = message.trim()
        )
    }

    private fun EasyTierSettings.normalize(): EasyTierSettings {
        return copy(
            instanceName = instanceName.trim().ifBlank { "ufi-remote" },
            networkName = networkName.trim(),
            networkSecret = networkSecret.trim(),
            hostname = hostname.trim(),
            virtualIpv4 = virtualIpv4.trim(),
            peers = peers.map { it.trim() }.filter { it.isNotBlank() },
            listeners = listeners.map { it.trim() }.filter { it.isNotBlank() },
            proxyNetworks = proxyNetworks.map { it.trim() }.filter { it.isNotBlank() },
            socks5Host = socks5Host.trim().ifBlank { "0.0.0.0" },
            socks5Port = socks5Port.coerceIn(1, 65535),
            logLevel = logLevel.trim().ifBlank { "info" }
        )
    }

    companion object {
        private const val PREFS_NAME = "ufi_remote_settings"
        private const val KEY_SETTINGS_JSON = "settings_json"
        const val MIN_REFRESH_MINUTES = 1
        const val MAX_REFRESH_MINUTES = 1440
    }
}
