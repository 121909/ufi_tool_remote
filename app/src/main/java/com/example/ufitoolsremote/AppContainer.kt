package com.example.ufitoolsremote

import android.content.Context
import com.example.ufitoolsremote.data.DeviceRepository
import com.example.ufitoolsremote.data.SettingsRepository
import com.example.ufitoolsremote.data.SmsCacheRepository
import com.example.ufitoolsremote.data.SmsRepository
import com.example.ufitoolsremote.network.UfiApiClient

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val smsCacheRepository = SmsCacheRepository(context)
    val apiClient = UfiApiClient()
    val deviceRepository = DeviceRepository(apiClient)
    val smsRepository = SmsRepository(apiClient)
}
