package com.example.quickshutdownphone.feature.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.quickshutdownphone.MyApp
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.reLockAndNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyAdmin : DeviceAdminReceiver()

class UserUnlockReceiver(
    val myApp: MyApp = MyApp.getInstance(),
    val sharedPrefApi: SharedPrefApi = myApp.sharedPrefApi,
): BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            job?.cancel()
            job = CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                context.reLockAndNotify(sharedPrefApi)
            }
        }
    }

    companion object {
        var job: Job? = null
    }
}