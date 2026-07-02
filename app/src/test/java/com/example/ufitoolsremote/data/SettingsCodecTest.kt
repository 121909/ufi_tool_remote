package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.AppSettings
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.EasyTierSettings
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import com.example.ufitoolsremote.model.UfiAccessMode
import com.example.ufitoolsremote.model.resolvedConnectionConfig
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

    @Test
    fun resolvedConnectionConfig_mapsWildcardEasyTierBindHostToLoopbackDialHost() {
        val settings = AppSettings(
            connection = ConnectionConfig(
                accessMode = UfiAccessMode.EasyTierSocks5,
                easyTierSocks5Host = "10.0.0.2",
                easyTierSocks5Port = 2080
            ),
            easyTier = EasyTierSettings(
                socks5Host = "0.0.0.0",
                socks5Port = 1180
            )
        )

        val resolved = settings.resolvedConnectionConfig()

        assertEquals("127.0.0.1", resolved.easyTierSocks5Host)
        assertEquals(1180, resolved.easyTierSocks5Port)
    }
}
