package com.example.ufitoolsremote.easytier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ufitoolsremote.UfiRemoteApplication

class EasyTierBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? UfiRemoteApplication ?: return
        if (!app.container.settingsRepository.current().easyTier.enabled) return
        runCatching { EasyTierService.start(context.applicationContext) }
    }
}
