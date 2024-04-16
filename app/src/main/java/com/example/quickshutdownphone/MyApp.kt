package com.example.quickshutdownphone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.startAllBroadcastStarters

class MyApp : Application() {
    val sharedPrefApi: SharedPrefApi by lazy {
        SharedPrefApi(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val descriptionText = "This is my channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        startAllBroadcastStarters()
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