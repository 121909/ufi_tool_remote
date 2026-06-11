package com.example.ufitoolsremote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ufitoolsremote.ComposeSmsActivity
import com.example.ufitoolsremote.MainActivity
import com.example.ufitoolsremote.R
import com.example.ufitoolsremote.SmsDetailActivity
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UfiRemoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id))
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_sms_list)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val settings = (context.applicationContext as UfiRemoteApplication).container.settingsRepository.current()
        WidgetScheduler.schedule(context, settings.widgetRefreshMinutes)
        updateWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetActions.ACTION_REFRESH -> WidgetScheduler.refreshNow(context)
            WidgetActions.ACTION_OPEN_SMS_DETAIL -> openSmsDetail(context, intent)
            WidgetActions.ACTION_OPEN_APP -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launch)
            }
            WidgetActions.ACTION_QUICK_REPLY -> handleQuickReply(context, intent)
        }
    }

    companion object {
        fun updateWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UfiRemoteWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> manager.updateAppWidget(id, buildRemoteViews(context, id)) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_sms_list)
        }

        private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
            val app = context.applicationContext as UfiRemoteApplication
            val settings = app.container.settingsRepository.current()
            val cache = app.container.smsCacheRepository.load()
            val views = RemoteViews(context.packageName, R.layout.widget_ufi_remote)

            views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
            views.setTextViewText(
                R.id.widget_status,
                buildStatusText(settings.widgetRefreshMinutes, cache.updatedAtMillis, cache.statusText, cache.statusAtMillis)
            )
            views.setTextViewText(R.id.widget_sms_summary, buildSmsSummary(cache.messages.size, cache.messages.count { it.isUnread }))
            views.setTextViewText(R.id.widget_quick_reply_summary, buildQuickReplySummary(settings.quickReplies.size))
            views.setOnClickPendingIntent(
                R.id.widget_refresh_button,
                pendingBroadcast(context, WidgetActions.ACTION_REFRESH, widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.widget_open_button,
                pendingBroadcast(context, WidgetActions.ACTION_OPEN_APP, widgetId)
            )
            views.setRemoteAdapter(
                R.id.widget_sms_list,
                Intent(context, WidgetSmsListRemoteViewsService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
            )
            views.setEmptyView(R.id.widget_sms_list, R.id.widget_sms_empty)
            views.setPendingIntentTemplate(
                R.id.widget_sms_list,
                PendingIntent.getBroadcast(
                    context,
                    widgetId + 4000,
                    Intent(context, UfiRemoteWidgetProvider::class.java).apply {
                        action = WidgetActions.ACTION_OPEN_SMS_DETAIL
                    },
                    pendingFlags(mutable = true)
                )
            )
            renderQuickReplyChips(context, views, widgetId, settings.quickReplies)
            return views
        }

        private fun renderQuickReplyChips(context: Context, views: RemoteViews, widgetId: Int, presets: List<QuickReplyPreset>) {
            views.removeAllViews(R.id.widget_quick_reply_container)
            if (presets.isEmpty()) {
                val empty = RemoteViews(context.packageName, R.layout.widget_quick_reply_chip)
                empty.setTextViewText(R.id.widget_quick_reply_label, "添加快捷回复")
                empty.setTextViewText(R.id.widget_quick_reply_meta, "在应用的设置页配置")
                empty.setOnClickPendingIntent(
                    R.id.widget_quick_reply_root,
                    pendingBroadcast(context, WidgetActions.ACTION_OPEN_APP, 9000)
                )
                views.addView(R.id.widget_quick_reply_container, empty)
                return
            }

            val visible = presets.take(MAX_VISIBLE_QUICK_REPLIES)
            visible.forEachIndexed { index, preset ->
                val chip = RemoteViews(context.packageName, R.layout.widget_quick_reply_chip)
                chip.setTextViewText(R.id.widget_quick_reply_label, preset.label.ifBlank { "快捷回复" })
                chip.setTextViewText(
                    R.id.widget_quick_reply_meta,
                    listOfNotNull(preset.number.takeIf { it.isNotBlank() }, preset.message.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifBlank { if (preset.sendMode == QuickReplySendMode.Direct) "直接发送" else "确认发送" }
                )
                chip.setOnClickPendingIntent(
                    R.id.widget_quick_reply_root,
                    pendingQuickReply(context, widgetId, preset, index)
                )
                views.addView(R.id.widget_quick_reply_container, chip)
            }

            if (presets.size > MAX_VISIBLE_QUICK_REPLIES) {
                val overflow = presets.size - MAX_VISIBLE_QUICK_REPLIES
                val more = RemoteViews(context.packageName, R.layout.widget_quick_reply_chip)
                more.setTextViewText(R.id.widget_quick_reply_label, "+$overflow")
                more.setTextViewText(R.id.widget_quick_reply_meta, "更多快捷回复")
                more.setOnClickPendingIntent(
                    R.id.widget_quick_reply_root,
                    pendingBroadcast(context, WidgetActions.ACTION_OPEN_APP, 9001)
                )
                views.addView(R.id.widget_quick_reply_container, more)
            }
        }

        private fun handleQuickReply(context: Context, intent: Intent) {
            val app = context.applicationContext as UfiRemoteApplication
            val number = intent.getStringExtra(WidgetActions.EXTRA_NUMBER).orEmpty()
            val message = intent.getStringExtra(WidgetActions.EXTRA_MESSAGE).orEmpty()
            val label = intent.getStringExtra(WidgetActions.EXTRA_LABEL).orEmpty().ifBlank { "快捷回复" }
            val sendMode = intent.getStringExtra(WidgetActions.EXTRA_SEND_MODE).orEmpty()
            if (number.isBlank() || message.isBlank()) {
                app.container.smsCacheRepository.updateStatus("快捷回复缺少号码或内容")
                updateWidgets(context)
                return
            }

            if (sendMode == WidgetActions.QUICK_REPLY_SEND_MODE_DIRECT) {
                app.container.smsCacheRepository.updateStatus("正在发送：$label")
                updateWidgets(context)
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

        private fun pendingQuickReply(context: Context, widgetId: Int, preset: QuickReplyPreset, index: Int): PendingIntent {
            val requestCode = widgetId * 100 + 7000 + index
            val intent = Intent(context, UfiRemoteWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_QUICK_REPLY
                putExtra(WidgetActions.EXTRA_NUMBER, preset.number)
                putExtra(WidgetActions.EXTRA_MESSAGE, preset.message)
                putExtra(WidgetActions.EXTRA_LABEL, preset.label)
                putExtra(
                    WidgetActions.EXTRA_SEND_MODE,
                    if (preset.sendMode == QuickReplySendMode.Direct) {
                        WidgetActions.QUICK_REPLY_SEND_MODE_DIRECT
                    } else {
                        WidgetActions.QUICK_REPLY_SEND_MODE_CONFIRM
                    }
                )
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags())
        }

        private fun pendingBroadcast(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, UfiRemoteWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags())
        }

        private fun pendingFlags(mutable: Boolean = false): Int {
            return widgetPendingIntentFlags(mutable)
        }

        private fun buildStatusText(
            refreshMinutes: Int,
            updatedAtMillis: Long,
            statusText: String,
            statusAtMillis: Long
        ): String {
            val updated = if (updatedAtMillis > 0L) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updatedAtMillis))
            } else {
                "未刷新"
            }
            val action = if (statusText.isNotBlank()) {
                val time = if (statusAtMillis > 0L) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(statusAtMillis))
                } else {
                    ""
                }
                if (time.isBlank()) statusText else "$statusText $time"
            } else {
                "就绪"
            }
            return "自动 ${refreshMinutes}m · 刷新 $updated · $action"
        }

        private fun buildSmsSummary(total: Int, unread: Int): String {
            return when {
                unread > 0 -> "未读 $unread"
                total > 0 -> "$total 条"
                else -> "空"
            }
        }

        private fun buildQuickReplySummary(count: Int): String {
            return when (count) {
                0 -> "未配置"
                1 -> "1 个"
                else -> "$count 个"
            }
        }

        private const val MAX_VISIBLE_QUICK_REPLIES = 4
    }
}

internal fun widgetPendingIntentFlags(mutable: Boolean, sdkInt: Int = Build.VERSION.SDK_INT): Int {
    val mutability = when {
        mutable && sdkInt >= Build.VERSION_CODES.S -> PendingIntent.FLAG_MUTABLE
        mutable -> 0
        else -> PendingIntent.FLAG_IMMUTABLE
    }
    return PendingIntent.FLAG_UPDATE_CURRENT or mutability
}
