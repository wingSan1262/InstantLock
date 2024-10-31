package com.risyan.quickshutdownphone.feature.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


interface ScreenShotService {
    fun takeScreenShot(
        onImage: (Bitmap) -> Unit,
    )
}

class ScreenShotServiceImp(
    val owner: AccessibilityService,
    val ownerScope: CoroutineScope
) : ScreenShotService {
    @SuppressLint("InlinedApi")
    override fun takeScreenShot(onImage: (Bitmap) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.takeScreenshotAndroidR(onImage)
        } else {
            takeScreenShotOldWay(onImage)
        }
    }

    fun takeScreenShotOldWay(
        onImage: (Bitmap) -> Unit
    ) {
        owner.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        ownerScope.launch(Dispatchers.IO) {
            delay(500);
            getLatestScreenshotBitmap(owner.contentResolver)?.let {
                withContext(Dispatchers.Main) {
                    onImage(it)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotAndroidR(
        onImage: (Bitmap) -> Unit
    ) {
        owner.takeScreenshot(
            DEFAULT_DISPLAY,
            { r -> Thread(r).start() },
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer,
                        screenshot.colorSpace
                    )?.copy(Bitmap.Config.ARGB_8888, false)
                    screenshot.hardwareBuffer.close()

                    if (bitmap == null) {
                        takeScreenShotOldWay(onImage)
                    } else {
                        ownerScope.launch(Dispatchers.Main) {
                            onImage(bitmap);
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.e(
                        "Take screenshot: ",
                        "takeScreenshot() -> onFailure($errorCode), falling back to GLOBAL_ACTION_TAKE_SCREENSHOT"
                    )
                    takeScreenShotOldWay(onImage)
                }
            })
    }

    fun deleteScreenshotById(contentResolver: ContentResolver, id: Long): Boolean {
        // Construct the URI for the media item based on its ID
        val uri = Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id.toString()
        )

        // Try to delete the media file using the content resolver
        val rowsDeleted = contentResolver.delete(uri, null, null)

        // Return true if one or more rows were deleted, otherwise false
        return rowsDeleted > 0
    }

    fun getLatestScreenshotBitmap(contentResolver: ContentResolver): (Bitmap)? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                val bitmap = getBitmapFromUri(contentResolver, uri)
                deleteScreenshotById(contentResolver, id)
                return bitmap
            }
        }
        return null
    }

    private fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        } catch (e: Exception) {
            Log.e("BitmapError", "Error getting bitmap: ${e.message}")
            null
        }
    }
}