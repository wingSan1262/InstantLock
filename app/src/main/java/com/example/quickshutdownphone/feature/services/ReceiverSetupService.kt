package com.example.quickshutdownphone.feature.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.example.quickshutdownphone.MyApp
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.doLock
import com.example.quickshutdownphone.feature.reLockAndNotify
import com.example.quickshutdownphone.feature.receivers.PowerButtonReceiver
import com.example.quickshutdownphone.feature.receivers.UserUnlockReceiver
import com.example.quickshutdownphone.feature.showNotification
import com.example.quickshutdownphone.feature.startLockingAndNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReceiverSetupService(
    val myApp: MyApp = MyApp.getInstance(),
    val sharedPrefApi: SharedPrefApi = myApp.sharedPrefApi,
): Service() {
    private lateinit var powerButtonReceiver: PowerButtonReceiver
    private lateinit var userUnlockReceiver: UserUnlockReceiver

    fun addPeriodicServiceStarter(){
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReceiverSetupService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pendingIntent)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, showNotification("Instant Lockdown is ready"))
        powerButtonReceiver = PowerButtonReceiver()
        userUnlockReceiver = UserUnlockReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT) // Add this line to listen for user unlock events
        }
        registerReceiver(powerButtonReceiver, filter)
        registerReceiver(userUnlockReceiver, filter) // Register the UserUnlockReceiver
        CoroutineScope(Dispatchers.Main).launch {
            reLockAndNotify(sharedPrefApi)
        }
        addPeriodicServiceStarter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerButtonReceiver)
        unregisterReceiver(userUnlockReceiver) // Unregister the UserUnlockReceiver
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}