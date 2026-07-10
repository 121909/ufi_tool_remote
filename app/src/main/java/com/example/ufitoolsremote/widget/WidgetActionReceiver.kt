package com.example.ufitoolsremote.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ufitoolsremote.ComposeSmsActivity
import com.example.ufitoolsremote.MainActivity
import com.example.ufitoolsremote.SmsDetailActivity
import com.example.ufitoolsremote.UfiRemoteApplication

/** Handles explicit PendingIntents created by the widget and is not exported. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetActions.ACTION_REFRESH -> WidgetScheduler.refreshNow(context)
            WidgetActions.ACTION_OPEN_SMS_DETAIL -> openSmsDetail(context, intent)
            WidgetActions.ACTION_OPEN_APP -> openApp(context)
            WidgetActions.ACTION_QUICK_REPLY -> handleQuickReply(context, intent)
        }
    }

    private fun openApp(context: Context) {
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(launch)
    }

    private fun handleQuickReply(context: Context, intent: Intent) {
        val app = context.applicationContext as UfiRemoteApplication
        val number = intent.getStringExtra(WidgetActions.EXTRA_NUMBER).orEmpty()
        val message = intent.getStringExtra(WidgetActions.EXTRA_MESSAGE).orEmpty()
        val label = intent.getStringExtra(WidgetActions.EXTRA_LABEL).orEmpty().ifBlank { "快捷回复" }
        val sendMode = intent.getStringExtra(WidgetActions.EXTRA_SEND_MODE).orEmpty()
        if (number.isBlank() || message.isBlank()) {
            app.container.smsCacheRepository.updateStatus("快捷回复缺少号码或内容")
            UfiRemoteWidgetProvider.updateWidgets(context)
            return
        }

        if (sendMode == WidgetActions.QUICK_REPLY_SEND_MODE_DIRECT) {
            app.container.smsCacheRepository.updateStatus("正在发送：$label")
            UfiRemoteWidgetProvider.updateWidgets(context)
            val work = OneTimeWorkRequestBuilder<WidgetSendQuickReplyWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(WidgetActions.EXTRA_NUMBER, number)
                        .putString(WidgetActions.EXTRA_MESSAGE, message)
                        .putString(WidgetActions.EXTRA_LABEL, label)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(work)
            return
        }

        val launch = Intent(context, ComposeSmsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(WidgetActions.EXTRA_NUMBER, number)
            putExtra(WidgetActions.EXTRA_MESSAGE, message)
            putExtra(WidgetActions.EXTRA_LABEL, label)
            putExtra(WidgetActions.EXTRA_FROM_WIDGET, true)
        }
        context.startActivity(launch)
    }

    private fun openSmsDetail(context: Context, intent: Intent) {
        val launch = Intent(context, SmsDetailActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(WidgetActions.EXTRA_MESSAGE_ID, intent.getStringExtra(WidgetActions.EXTRA_MESSAGE_ID).orEmpty())
            putExtra(WidgetActions.EXTRA_NUMBER, intent.getStringExtra(WidgetActions.EXTRA_NUMBER).orEmpty())
            putExtra(WidgetActions.EXTRA_MESSAGE, intent.getStringExtra(WidgetActions.EXTRA_MESSAGE).orEmpty())
            putExtra(WidgetActions.EXTRA_DATE, intent.getStringExtra(WidgetActions.EXTRA_DATE).orEmpty())
            putExtra(WidgetActions.EXTRA_FROM_WIDGET, true)
        }
        context.startActivity(launch)
    }
}
