package com.example.quickshutdownphone.feature

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.quickshutdownphone.R
import com.example.quickshutdownphone.data.LockStatus
import com.example.quickshutdownphone.data.SharedPrefApi
import com.example.quickshutdownphone.feature.receivers.MyAdmin
import com.example.quickshutdownphone.feature.receivers.ShutdownType
import com.example.quickshutdownphone.feature.services.ReceiverSetupService
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit


fun Context.showToast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}

fun getCurrentDatePlusSeconds(seconds: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.SECOND, seconds)
    return calendar.timeInMillis
}

fun getSecondsDiff(date1: Long, date2: Long): Long {
    val diffInMillis = Math.abs(date1 - date2)
    return TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
}

@SuppressLint("MissingPermission")
fun Context.showNotification(message: String): Notification {
    val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
        .setSmallIcon(R.mipmap.ic_launcher_monochrome)
        .setContentTitle("Instant Lockdown")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(this)) {
        val notificationId = 1
        val notification = builder.build()
        notify(notificationId, notification)
        return@showNotification notification
    }
}

suspend fun Context.startLockingAndNotify(
    sharedPrefApi: SharedPrefApi,
    type: ShutdownType
) {
    val lockStatus = sharedPrefApi.getLockStatus()
    if (lockStatus?.startLock == true && lockStatus.endLock > System.currentTimeMillis()) {
        return
    }
    val newLockStatus = LockStatus(
        true,
        getCurrentDatePlusSeconds(type.duration)
    )
    showNotification("Your phone will be locked for ${newLockStatus.getRemainingDurationTo()}")
    showSystemAnnouncement(
        "Instant Lockdown",
        "Your phone will be locked for ${newLockStatus.getRemainingDurationTo()}",
        "OK"
    )
    sharedPrefApi.saveLockStatus(
        newLockStatus
    )
    delay(2000)
    doLock()
}

fun Context.startAllBroadcastStarters() {
    val serviceIntent = Intent(this, ReceiverSetupService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(serviceIntent)
    } else {
        this.startService(serviceIntent)
    }
}

suspend fun Context.reLockAndNotify(
    sharedPrefApi: SharedPrefApi,
) {
    val lockStatus = sharedPrefApi.getLockStatus()
    if (lockStatus?.startLock == true && lockStatus.endLock > System.currentTimeMillis()) {
        showNotification("Lockdown time remaining is ${lockStatus.getRemainingDurationTo()}")
        val dialog = showSystemAnnouncement(
            "Instant Lockdown",
            "Lockdown time remaining is ${lockStatus.getRemainingDurationTo()}",
            "OK"
        )
        delay(2000)
        doLock()
        delay(2000)
        dialog?.dismiss()
        return
    }
    sharedPrefApi.removeLockStatus()
}

fun Long.toMinuteAndSecondFormat(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d minutes and %02d seconds", minutes, seconds)
}

fun Context.showSystemAnnouncement(
    title: String,
    message: String,
    positiveButton: String,
): AlertDialog? {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButton) { dialog, _ -> dialog.dismiss() }
    builder.setOnDismissListener { }
    val dialog = builder.create()
    val params = dialog.window?.attributes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        params?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        params?.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    }
    dialog.window?.attributes = params
    dialog.show()
    return dialog
}


fun Context.doLock() {
    val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val compName = ComponentName(this, MyAdmin::class.java)

    val active = devicePolicyManager.isAdminActive(compName)
    if (active) {
        devicePolicyManager.lockNow()
    }
}

fun Context.hasNotificationAccess(): Boolean {
    if (Build.VERSION.SDK_INT >= 33) {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    return true
}

fun Context.requestNotificationAccess() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.requestPermissions(
            this as ComponentActivity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            101
        )
    };
}

fun Context.openAdminPermissionSetting() {
    val intent = Intent().apply {
        setComponent(
            ComponentName(
                "com.android.settings",
                "com.android.settings.DeviceAdminSettings"
            )
        )
    }
    startActivity(intent)
}

fun Context.hasAdminPermission(): Boolean {
    val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val compName = ComponentName(this, MyAdmin::class.java)
    return devicePolicyManager.isAdminActive(compName)
}

fun Context.openOverlayPermissionSetting() {
    val intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
    startActivity(intent)
}

fun Context.hasOverlayPermission(): Boolean {
    return Settings.canDrawOverlays(this)
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}