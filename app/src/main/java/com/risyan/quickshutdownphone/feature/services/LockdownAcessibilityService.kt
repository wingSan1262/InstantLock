package com.risyan.quickshutdownphone.feature.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.risyan.quickshutdownphone.MyApp
import com.risyan.quickshutdownphone.feature.checkJobAndSaveLockStatus
import com.risyan.quickshutdownphone.feature.hasStorageAccessNeededInstantLock
import com.risyan.quickshutdownphone.feature.model.ShutdownType
import com.risyan.quickshutdownphone.feature.showSystemAnnouncement
import com.risyan.quickshutdownphone.feature.startUnlockReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockdownAcessibilityService : AccessibilityService() {

    val scope = CoroutineScope(Dispatchers.IO)
    val aiNsfwGrader = AiNsfwGraderImp(this, scope)
    val blankImageChecker = NonScreenShotImageGraderImp(scope)
    val screenShotService = ScreenShotServiceImp(this, scope)
    val sharedPrefApi = MyApp.getInstance().sharedPrefApi
    val userSetting = MyApp.getInstance().userSetting

    var periodicScreenShotJob: Job? = null

    val SCREENSHOT_INTERVAL = 30000L

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        initPeriodicScreenShot()
        startUnlockReceiver()
    }

    fun initPeriodicScreenShot() {
        periodicScreenShotJob?.cancel();
        periodicScreenShotJob = scope.launch {
            delay(250L)
            if (!hasStorageAccessNeededInstantLock() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                delay(SCREENSHOT_INTERVAL * 20)
                this@LockdownAcessibilityService.showSystemAnnouncement(
                    "Please grant storage permission",
                    "You don't have storage permission, please grant it to continue using the app",
                    "OK"
                )
                initPeriodicScreenShot()
                return@launch
            }
            doScreenShot()
            delay(SCREENSHOT_INTERVAL)
            initPeriodicScreenShot()
        }
    }

    fun doNsfwCheck(bitmap: Bitmap) {
        aiNsfwGrader.checkIfNsfw(bitmap) { isNsfw, isSexy ->
            scope.launch(Dispatchers.Main) {
                if (isNsfw && userSetting.lockByNsfw) {
                    checkJobAndSaveLockStatus(ShutdownType.QUICK_5_MINUTES_NFSW, sharedPrefApi)
                    return@launch
                }

                if (isSexy && userSetting.lockBySexy) {
                    checkJobAndSaveLockStatus(
                        ShutdownType.QUICK_3_MINUTES_SEXY, sharedPrefApi
                    )
                }
            }
        }
    }

    fun checkIfBlankThenNsfw(bitmap: Bitmap) {
        blankImageChecker.checkIfMostBlackImageIsLockable(bitmap, {
            checkJobAndSaveLockStatus(ShutdownType.QUICK_3_MINUTES_SEXY, sharedPrefApi)
        }, {
            doNsfwCheck(bitmap)
        })
    }
    fun doScreenShot() {
        screenShotService.takeScreenShot { bitmap ->
            checkIfBlankThenNsfw(bitmap)
        }
    }

}



