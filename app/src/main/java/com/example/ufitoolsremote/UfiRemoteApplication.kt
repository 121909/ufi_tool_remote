package com.example.ufitoolsremote

import android.app.Application

class UfiRemoteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
