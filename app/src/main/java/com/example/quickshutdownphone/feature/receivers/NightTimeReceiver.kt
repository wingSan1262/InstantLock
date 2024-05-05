package com.example.quickshutdownphone.feature.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.quickshutdownphone.MyApp
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.startLockingAndNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NightTimeReceiver(
    val myApp: MyApp = MyApp.getInstance(),
    val sharedPrefApi: SharedPrefApi = myApp.sharedPrefApi,
): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        sharedPrefApi.removeLockStatus()
        CoroutineScope(Dispatchers.Main).launch {
            context.startLockingAndNotify(
                sharedPrefApi, ShutdownType.NIGHT_4HOUR_TIME
            )
        }
    }
}