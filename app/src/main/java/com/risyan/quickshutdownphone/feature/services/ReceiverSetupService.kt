package com.risyan.quickshutdownphone.feature.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.risyan.quickshutdownphone.MyApp
import com.risyan.quickshutdownphone.base.data.SharedPrefApi
import com.risyan.quickshutdownphone.feature.reLockAndNotify
import com.risyan.quickshutdownphone.feature.receivers.UserUnlockReceiver
import com.risyan.quickshutdownphone.feature.startUnlockReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReceiverSetupService(
    val myApp: MyApp = MyApp.getInstance(),
    val sharedPrefApi: SharedPrefApi = myApp.sharedPrefApi,
): Service() {

    lateinit var userUnlockReceiver: UserUnlockReceiver
    override fun onCreate() {
        super.onCreate()
        userUnlockReceiver = startUnlockReceiver()
        CoroutineScope(Dispatchers.Main).launch {
            reLockAndNotify(sharedPrefApi)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(userUnlockReceiver) // Unregister the UserUnlockReceiver
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}