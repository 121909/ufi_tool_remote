package com.example.ufitoolsremote.widget

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
        val work = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMinutes.toLong().coerceAtLeast(0L), TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, work)
    }
}
