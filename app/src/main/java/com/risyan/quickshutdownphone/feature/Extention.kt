package com.risyan.quickshutdownphone.feature

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.data.LockStatus
import com.risyan.quickshutdownphone.data.SharedPrefApi
import com.risyan.quickshutdownphone.feature.receivers.MyAdmin
import com.risyan.quickshutdownphone.feature.receivers.NightTimeReceiver
import com.risyan.quickshutdownphone.feature.receivers.ShutdownType
import com.risyan.quickshutdownphone.feature.services.ReceiverSetupService
import com.risyan.quickshutdownphone.feature.widget.LockWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        return
    }
    val newLockStatus = LockStatus(
        true,
        getCurrentDatePlusSeconds(duration)
    )
    showNotification(getString(R.string.lock_notification_label) + newLockStatus.getRemainingDurationTo(this))
    val dialog = showSystemAnnouncement(
        getString(R.string.app_name),
        getString(R.string.lock_notification_header) + newLockStatus.getRemainingDurationTo(this),
        "OK"
    )
    sharedPrefApi.saveLockStatus(
        newLockStatus
    )
    delay(2000)
    dialog?.dismiss()
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
        showNotification(getString(R.string.time_remaining) + lockStatus.getRemainingDurationTo(this))
        val dialog = showSystemAnnouncement(
            getString(R.string.app_name),
            getString(R.string.time_remaining) + lockStatus.getRemainingDurationTo(this),
            "OK"
        )
        delay(2000)
        dialog?.dismiss()
        delay(1000)
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
    return minutes.toString() + context.getString(R.string.minutes_and_label) + seconds.toString() + "seconds"
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

@SuppressLint("ClickableViewAccessibility")
fun Context.createOverlay(
    on2SecondHold: () -> Unit,
    on4SecondHold: () -> Unit,
) {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val overlayView = LockWidget(this, on2SecondHold = on2SecondHold, on4SecondHold = on4SecondHold)

    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    params.gravity = Gravity.TOP or Gravity.START
    params.x = 0
    params.y = 250

    windowManager.addView(overlayView, params)

    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f
    var lastAction = 0



    overlayView.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                lastAction = event.action
                overlayView.setNotTranslucentForAMoment()
                return@setOnTouchListener false
            }

            MotionEvent.ACTION_UP -> {
                lastAction = event.action
                overlayView.setNotTranslucentForAMoment()
                return@setOnTouchListener false
            }

            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(v, params)
                lastAction = event.action
                overlayView.setNotTranslucentForAMoment()
                return@setOnTouchListener false
            }

            else -> false
        }
    }

    overlayView.setOnClickListener {
        showLockDialogQuestion(
            getString(R.string.app_name),
            getString(R.string.message_lock_confirm_question),
            on2SecondHold,
            on4SecondHold
        )
    }


    overlayView.setNotTranslucentForAMoment()
}

fun Context.isVivoDevice(): Boolean {
    return Build.MANUFACTURER.equals("vivo", ignoreCase = true)
}

// is xiaomi device
fun Context.isXiaomiDevice(): Boolean {
    return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)
}

// is huawei device
fun Context.isHuaweiDevice(): Boolean {
    return Build.MANUFACTURER.equals("huawei", ignoreCase = true)
}

// is oppo device
fun Context.isOppoDevice(): Boolean {
    return Build.MANUFACTURER.equals("oppo", ignoreCase = true)
}

// is letv device
fun Context.isLetvDevice(): Boolean {
    return Build.MANUFACTURER.equals("letv", ignoreCase = true)
}

// is honor device
fun Context.isHonorDevice(): Boolean {
    return Build.MANUFACTURER.equals("honor", ignoreCase = true)
}

fun Context.isManufactureAdditionalSetting(): Boolean {
    return isVivoDevice() || isXiaomiDevice() || isHuaweiDevice() ||
            isOppoDevice() || isLetvDevice() || isHonorDevice()
}

// open xiaomi auto start setting
fun Context.autoLaunchXiaomiSetting() {
    try {
        val intent = Intent()
        intent.component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// open letv auto start setting
fun Context.autoLaunchLetvSetting() {
    try {
        val intent = Intent()
        intent.component = ComponentName(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        )
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// open honor auto start setting
fun Context.autoLaunchHonorSetting() {
    try {
        val intent = Intent()
        intent.component = ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// open oppo auto start setting
fun Context.autoLaunchOppoSetting() {
    try {
        val intent = Intent()
        intent.component = ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        )
        startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
            startActivity(intent)
        } catch (ex: Exception) {
            try {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
                startActivity(intent)
            } catch (exx: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

// Open Setting
fun Context.autoLaunchVivoSetting() {
    try {
        val intent = Intent()
        intent.setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        )
        startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent()
            intent.setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            )
            startActivity(intent)
        } catch (ex: Exception) {
            try {
                val intent = Intent()
                intent.setClassName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
                startActivity(intent)
            } catch (exx: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

// open url
fun Context.openUrl(url: String) {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(url)
    startActivity(i)
}