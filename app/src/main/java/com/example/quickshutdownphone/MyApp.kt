package com.example.quickshutdownphone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.startSingleAllBroadcastStarters

class MyApp : Application() {
    val sharedPrefApi: SharedPrefApi by lazy {
        SharedPrefApi(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
    }

    init {
        instance = this
    }

    companion object {
        private var instance: MyApp? = null

        fun getInstance(): MyApp =
            instance ?: throw IllegalStateException("MyApp has not been created yet")
    }
}