package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.AppSettings
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {
    @Test
    fun settings_roundTripPreservesConnectionAndQuickReplies() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val original = AppSettings(
            connection = ConnectionConfig(
                baseUrl = "http://10.0.0.1:2333",
                ufiToken = "token",
                adminPassword = "admin",
                loginModePreference = LoginModePreference.LegacyFirst
            ),
            widgetRefreshMinutes = 18,
            quickReplies = listOf(
                QuickReplyPreset(
                    id = "preset-1",
                    label = "问候",
                    number = "10086",
                    message = "你好",
                    sendMode = QuickReplySendMode.Direct
                )
            )
        )

        val decoded = json.decodeFromString(AppSettings.serializer(), json.encodeToString(AppSettings.serializer(), original))

        assertEquals(original, decoded)
    }
}
