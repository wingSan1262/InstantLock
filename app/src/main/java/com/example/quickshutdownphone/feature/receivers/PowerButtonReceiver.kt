package com.example.quickshutdownphone.feature.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.quickshutdownphone.MyApp
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.startLockingAndNotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


enum class ShutdownType(val duration: Int) {
    QUICK_5_MINUTES(
//        5
        5 * 60
    ),
    MEDIUM_10_MINUTES(
//        10
        10 * 60
    ),
    LONG_20_MINUTES(
//        15
        30 * 60
    ),
}
class PowerButtonReceiver(
    val myApp: MyApp = MyApp.getInstance(),
    val sharedPrefApi: SharedPrefApi = myApp.sharedPrefApi,
): BroadcastReceiver() {

    fun checkAndSaveLockStatus(
        context: Context,
        type: ShutdownType
    ){
        lockerJob?.cancel()
        lockerJob = CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            context.startLockingAndNotify(sharedPrefApi, type)
        }
    }

    fun resetJobCounter(){
        jobCounter?.cancel()
        jobCounter = CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            countPowerOff = 0
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_SCREEN_OFF == intent.action || Intent.ACTION_SCREEN_ON == intent.action) {
            countPowerOff++
            if (countPowerOff >= 4 && countPowerOff <= 5) {
                checkAndSaveLockStatus(context, ShutdownType.QUICK_5_MINUTES)
            } else if (countPowerOff > 6 && countPowerOff <= 7) {
                checkAndSaveLockStatus(context, ShutdownType.MEDIUM_10_MINUTES)
            } else if (countPowerOff >= 8) {
                checkAndSaveLockStatus(context, ShutdownType.LONG_20_MINUTES)
            }
            resetJobCounter()
        }
    }

    companion object {
        private var countPowerOff = 0
        var jobCounter: Job? = null
        var lockerJob: Job? = null
    }
}
