package com.example.ufitoolsremote.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.messageOrNull
import com.example.ufitoolsremote.model.resolvedConnectionConfig

class WidgetSendQuickReplyWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? UfiRemoteApplication ?: return Result.failure()
        val connection = app.container.settingsRepository.current().resolvedConnectionConfig()
        val number = inputData.getString(WidgetActions.EXTRA_NUMBER).orEmpty()
        val message = inputData.getString(WidgetActions.EXTRA_MESSAGE).orEmpty()
        val label = inputData.getString(WidgetActions.EXTRA_LABEL).orEmpty().ifBlank { "快捷回复" }
        if (number.isBlank() || message.isBlank()) {
            app.container.smsCacheRepository.updateStatus("快捷回复缺少号码或内容")
            WidgetUpdater.updateAll(applicationContext)
            return Result.failure()
        }

        app.container.smsCacheRepository.updateStatus("正在发送：$label")
        WidgetUpdater.updateAll(applicationContext)

        return when (val result = app.container.smsRepository.sendSms(connection, number, message)) {
            is ApiResult.Success -> {
                app.container.smsCacheRepository.updateStatus("已发送：$label")
                WidgetUpdater.updateAll(applicationContext)
                WidgetScheduler.refreshNow(applicationContext)
                Result.success()
            }
            is ApiResult.Unauthorized -> {
                app.container.smsCacheRepository.updateStatus("发送失败：${result.messageOrNull().orEmpty()}")
                WidgetUpdater.updateAll(applicationContext)
                Result.failure()
            }
            is ApiResult.NetworkError -> {
                app.container.smsCacheRepository.updateStatus("发送失败：${result.message}")
                WidgetUpdater.updateAll(applicationContext)
                Result.failure()
            }
            is ApiResult.ParseError -> {
                app.container.smsCacheRepository.updateStatus("发送失败：${result.message}")
                WidgetUpdater.updateAll(applicationContext)
                Result.failure()
            }
            is ApiResult.DeviceError -> {
                app.container.smsCacheRepository.updateStatus("发送失败：${result.message}")
                WidgetUpdater.updateAll(applicationContext)
                Result.failure()
            }
        }
    }
}
