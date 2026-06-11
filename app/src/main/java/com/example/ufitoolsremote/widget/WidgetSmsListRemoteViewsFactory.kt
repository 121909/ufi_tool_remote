package com.example.ufitoolsremote.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.ufitoolsremote.R
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.model.SmsMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WidgetSmsListRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private val app = context.applicationContext as UfiRemoteApplication
    private var messages: List<SmsMessage> = emptyList()

    override fun onCreate() {
        messages = app.container.smsCacheRepository.loadMessages()
    }

    override fun onDataSetChanged() {
        messages = app.container.smsCacheRepository.loadMessages()
    }

    override fun onDestroy() {
        messages = emptyList()
    }

    override fun getCount(): Int = messages.size

    override fun getViewAt(position: Int): RemoteViews {
        val message = messages.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_sms_item)
        val views = RemoteViews(context.packageName, R.layout.widget_sms_item)
        views.setTextViewText(R.id.widget_sms_sender, message.number.ifBlank { "未知号码" })
        views.setTextViewText(R.id.widget_sms_content, message.content)
        views.setTextViewText(R.id.widget_sms_time, formatTime(message.date))
        views.setViewVisibility(R.id.widget_sms_unread, if (message.isUnread) android.view.View.VISIBLE else android.view.View.GONE)
        views.setOnClickFillInIntent(
            R.id.widget_sms_root,
            Intent().apply {
                action = WidgetActions.ACTION_OPEN_SMS_DETAIL
                putExtra(WidgetActions.EXTRA_MESSAGE_ID, message.id)
                putExtra(WidgetActions.EXTRA_NUMBER, message.number)
                putExtra(WidgetActions.EXTRA_MESSAGE, message.content)
                putExtra(WidgetActions.EXTRA_DATE, message.date)
                putExtra(WidgetActions.EXTRA_FROM_WIDGET, true)
            }
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = messages.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun formatTime(raw: String): String {
        return runCatching {
            val parts = raw.split(",")
            if (parts.size >= 5) {
                val now = Calendar.getInstance()
                val year = parts[0].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
                val month = parts[1].toIntOrNull()
                val day = parts[2].toIntOrNull()
                val hour = parts[3].padStart(2, '0')
                val minute = parts[4].padStart(2, '0')
                if (
                    year == now.get(Calendar.YEAR) &&
                    month == now.get(Calendar.MONTH) + 1 &&
                    day == now.get(Calendar.DAY_OF_MONTH)
                ) {
                    "$hour:$minute"
                } else {
                    "${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
                }
            } else {
                raw
            }
        }.getOrDefault(raw.ifBlank { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date()) })
    }
}
