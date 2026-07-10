package com.example.ufitoolsremote.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetScheduler {
    private const val WORK_NAME = "ufi_remote_widget_refresh"

    fun refreshNow(context: Context) {
        schedule(context, delayMinutes = 0)
    }

    fun schedule(context: Context, delayMinutes: Int) {
        val appContext = context.applicationContext
        if (!hasActiveWidgets(appContext)) {
            cancel(appContext)
            return
        }
        val work = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMinutes.toLong().coerceAtLeast(0L), TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, work)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    fun hasActiveWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context.applicationContext)
        val provider = ComponentName(context.applicationContext, UfiRemoteWidgetProvider::class.java)
        return shouldScheduleWidgetRefresh(manager.getAppWidgetIds(provider).size)
    }
}

internal fun shouldScheduleWidgetRefresh(activeWidgetCount: Int): Boolean = activeWidgetCount > 0
