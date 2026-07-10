package com.example.ufitoolsremote.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.messageOrNull
import com.example.ufitoolsremote.model.resolvedConnectionConfig

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!WidgetScheduler.hasActiveWidgets(applicationContext)) return Result.success()
        val app = applicationContext as? UfiRemoteApplication ?: return Result.failure()
        val settings = app.container.settingsRepository.current()
        val connection = settings.resolvedConnectionConfig()
        if (connection.normalizedBaseUrl.isBlank() || connection.ufiToken.isBlank() || connection.adminPassword.isBlank()) {
            app.container.smsCacheRepository.updateStatus("请先在应用里填写连接配置")
            WidgetUpdater.updateAll(applicationContext)
            WidgetScheduler.schedule(applicationContext, settings.widgetRefreshMinutes)
            return Result.success()
        }

        app.container.smsCacheRepository.updateStatus("正在刷新短信")
        WidgetUpdater.updateAll(applicationContext)

        return when (val result = app.container.smsRepository.fetchSms(connection)) {
            is ApiResult.Success -> {
                app.container.smsCacheRepository.save(
                    messages = result.value,
                    statusText = "短信已刷新",
                    statusAtMillis = System.currentTimeMillis()
                )
                WidgetUpdater.updateAll(applicationContext)
                WidgetScheduler.schedule(applicationContext, settings.widgetRefreshMinutes)
                Result.success()
            }
            else -> {
                app.container.smsCacheRepository.updateStatus("刷新失败：${result.messageOrNull().orEmpty()}")
                WidgetUpdater.updateAll(applicationContext)
                WidgetScheduler.schedule(applicationContext, settings.widgetRefreshMinutes)
                Result.success()
            }
        }
    }
}
