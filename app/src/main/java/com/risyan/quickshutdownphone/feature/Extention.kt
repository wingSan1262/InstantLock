package com.risyan.quickshutdownphone.feature

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.base.data.LockStatus
import com.risyan.quickshutdownphone.base.data.SharedPrefApi
import com.risyan.quickshutdownphone.feature.model.ShutdownType
import com.risyan.quickshutdownphone.feature.receivers.MyAdmin
import com.risyan.quickshutdownphone.feature.receivers.NightTimeReceiver
import com.risyan.quickshutdownphone.feature.receivers.UserUnlockReceiver
import com.risyan.quickshutdownphone.feature.services.LockdownAcessibilityService
import com.risyan.quickshutdownphone.feature.services.ReceiverSetupService
import com.risyan.quickshutdownphone.feature.widget.CustomBonkDialog
import com.risyan.quickshutdownphone.feature.widget.LockWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
        .setContentTitle(getString(R.string.app_name))
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(this)) {
        val notificationId = 1
        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ID",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = getString(R.string.channel_description)
            this.createNotificationChannel(channel)
        }

        notify(notificationId, notification)
        return@showNotification notification
    }
}

fun Context.checkJobAndSaveLockStatus(
    type: ShutdownType,
    sharedPrefApi: SharedPrefApi,
    job: Job? = null,
    delay: Long = 2000,
): Job {
    job?.cancel()
    return CoroutineScope(Dispatchers.Main).launch {
        delay(delay)
        startLockingAndNotify(sharedPrefApi, type)
    }
}

suspend fun Context.startLockingAndNotify(
    sharedPrefApi: SharedPrefApi,
    type: ShutdownType,
    duration: Int = type.duration,
) {
    val lockStatus = sharedPrefApi.getLockStatus()
    if (lockStatus?.startLock == true && lockStatus.endLock > System.currentTimeMillis()) {
        reLockAndNotify(sharedPrefApi)
        return
    }
    val newLockStatus = LockStatus(
        true,
        getCurrentDatePlusSeconds(duration)
    )
    val dialog = CustomBonkDialog(
        this,
        getString(type.titleId),
        getString(type.messageId) +
                "\n\n" +
                getString(R.string.punishment_time_remaining) + newLockStatus.getRemainingDurationTo(
            this
        ),
        type.imageId
    ) {}
    sharedPrefApi.saveLockStatus(
        newLockStatus
    )
    dialog.show()
    delay(type.timeUntilLock.toLong())
    dialog.dismiss()
    doLock()
}

fun Context.startSingleAllBroadcastStarters() {
    val serviceIntent = Intent(this, ReceiverSetupService::class.java)
    stopService(serviceIntent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(serviceIntent)
    } else {
        this.startService(serviceIntent)
    }
}

fun Service.startUnlockReceiver(): UserUnlockReceiver {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(1, this.showNotification(getString(R.string.instant_lockdown_is_ready)))
    } else {
        startForeground(1, this.showNotification(getString(R.string.instant_lockdown_is_ready)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }
    val userUnlockReceiver = UserUnlockReceiver()
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
    }
    registerReceiver(userUnlockReceiver, filter)
    return userUnlockReceiver;
}

fun Context.scheduleNightTimeLock() {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(this, NightTimeReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 30)
    }

    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

suspend fun Context.reLockAndNotify(
    sharedPrefApi: SharedPrefApi,
) {
    val lockStatus = sharedPrefApi.getLockStatus()
    if (lockStatus?.startLock == true && lockStatus.endLock > System.currentTimeMillis()) {
        val dialog = CustomBonkDialog(
            this,
            "You are being denied from this phone!",
            "You wandered too far and too \'fun\'. You'll being bonked from this phone!\n\n" +
                    "Punishment time remaining: " + lockStatus.getRemainingDurationTo(this),
            R.drawable.lockdown_notice
        ) {}
        dialog.show()
        delay(5000)
        dialog.dismiss()
        doLock()
        return
    }
    sharedPrefApi.removeLockStatus()
}

fun Long.toMinuteAndSecondFormat(
    context: Context
): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    //String.format("%02d minutes and %02d seconds", minutes, seconds)
    return minutes.toString() + context.getString(R.string.minutes_and_label) + seconds.toString() + " seconds"
}


fun Context.showLockDialogQuestion(
    title: String,
    message: String,
    on2SecondHold: () -> Unit,
    on4SecondHold: () -> Unit,
): AlertDialog? {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(getString(R.string.five_minute_label)) { dialog, _ ->
        on2SecondHold()
        dialog.dismiss()
    }
    builder.setNegativeButton(getString(R.string.twenty_minute_label)) { dialog, _ ->
        on4SecondHold()
        dialog.dismiss()
    }

    builder.setNeutralButton(getString(R.string.cancel_label)) { dialog, _ ->
        dialog.dismiss()
    }
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

fun Context.showSystemAnnouncement(
    title: String,
    message: String,
    positiveButton: String,
    onDismiss: (() -> Unit) = { }
): AlertDialog? {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButton) { dialog, _ -> dialog.dismiss() }
    builder.setOnDismissListener { onDismiss.invoke() }
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

fun Context.openOverlayPermissionSetting() {
    val intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
    startActivity(intent)
}

fun Context.hasOverlayPermission(): Boolean {
    return Settings.canDrawOverlays(this)
}

fun Context.hasAllInstantLockPermission() = hasAdminPermission() && hasNotificationAccess() &&
        hasAccessibilityService() && hasOverlayPermission() &&
        hasStorageAccessNeededInstantLock()

fun Context.hasStorageAccessNeededInstantLock(): Boolean {
    return hasStorageAccess() ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}

fun Context.hasStorageAccess(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.requestStorageAccess() {
    ActivityCompat.requestPermissions(
        this as ComponentActivity,
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ),
        101
    )
}

fun Context.hasAccessibilityService(): Boolean {
    val service = ComponentName(this, LockdownAcessibilityService::class.java)
    val accessibilityManager =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    val enabledServices =
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.contains(service.flattenToString()) == true
}

fun Context.requestAccessibilityService() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)
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

fun currentDatetoFormattedString(): String {
    val date = Date()
    val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    return formatter.format(date)
}
