package com.example.ufitoolsremote.data

import android.content.Context
import com.example.ufitoolsremote.model.SmsCacheSnapshot
import com.example.ufitoolsremote.model.SmsMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SmsCacheRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    fun load(): SmsCacheSnapshot = synchronized(lock) {
        val raw = prefs.getString(KEY_CACHE_JSON, null).orEmpty()
        if (raw.isBlank()) return SmsCacheSnapshot()
        runCatching { json.decodeFromString(SmsCacheSnapshot.serializer(), raw) }.getOrDefault(SmsCacheSnapshot())
    }

    fun loadMessages(): List<SmsMessage> = load().messages

    fun save(
        messages: List<SmsMessage>,
        updatedAtMillis: Long = System.currentTimeMillis(),
        statusText: String = "",
        statusAtMillis: Long = 0L
    ): SmsCacheSnapshot = synchronized(lock) {
        val current = load()
        val snapshot = SmsCacheSnapshot(
            messages = messages,
            updatedAtMillis = updatedAtMillis,
            statusText = statusText.ifBlank { current.statusText },
            statusAtMillis = if (statusAtMillis > 0L) statusAtMillis else current.statusAtMillis
        )
        prefs.edit().putString(KEY_CACHE_JSON, json.encodeToString(SmsCacheSnapshot.serializer(), snapshot)).apply()
        snapshot
    }

    fun clear(): SmsCacheSnapshot = save(emptyList(), 0L)

    fun updateStatus(statusText: String, statusAtMillis: Long = System.currentTimeMillis()): SmsCacheSnapshot = synchronized(lock) {
        val current = load()
        val snapshot = current.copy(statusText = statusText, statusAtMillis = statusAtMillis)
        prefs.edit().putString(KEY_CACHE_JSON, json.encodeToString(SmsCacheSnapshot.serializer(), snapshot)).apply()
        snapshot
    }

    companion object {
        private const val PREFS_NAME = "ufi_remote_sms_cache"
        private const val KEY_CACHE_JSON = "cache_json"
    }
}
