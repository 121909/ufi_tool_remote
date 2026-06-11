package com.example.ufitoolsremote.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetSmsListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetSmsListRemoteViewsFactory(applicationContext)
    }
}
