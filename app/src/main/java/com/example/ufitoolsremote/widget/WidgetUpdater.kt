package com.example.ufitoolsremote.widget

import android.content.Context

object WidgetUpdater {
    fun updateAll(context: Context) {
        UfiRemoteWidgetProvider.updateWidgets(context.applicationContext)
    }
}
