package com.example.quickshutdownphone.feature.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.quickshutdownphone.feature.services.ReceiverSetupService
import com.example.quickshutdownphone.feature.showNotification
import com.example.quickshutdownphone.feature.startAllBroadcastStarters

class PhonePowerOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            context.startAllBroadcastStarters()
        }
    }
}