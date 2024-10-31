package com.risyan.quickshutdownphone.feature.services

import android.graphics.Bitmap
import com.risyan.quickshutdownphone.MyApp
import com.risyan.quickshutdownphone.base.data.SharedPrefApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


interface NonScreenShotImageGrader {
    fun checkIfMostBlackImageIsLockable(
        bitmap: Bitmap,
        onBlankLock: () -> Unit,
        onNotBlank: () -> Unit
    )
}

class NonScreenShotImageGraderImp(
    val ownerScope: CoroutineScope,
    val sharedPreferences: SharedPrefApi = MyApp.getInstance().sharedPrefApi
) : NonScreenShotImageGrader {

    val BLANK_IMAGE_OCCURUANCE_THRESHOLD = 3

    fun hasDominantColor(bitmap: Bitmap, thresholdPercentage: Double = 80.0): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixelCount = width * height
        val colorCountMap = mutableMapOf<Int, Int>()

        // Loop through each pixel
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = bitmap.getPixel(x, y)
                colorCountMap[color] = colorCountMap.getOrDefault(color, 0) + 1
            }
        }

        // Check if any color exceeds the threshold
        for ((_, count) in colorCountMap) {
            val colorPercentage = (count.toDouble() / totalPixelCount) * 100
            if (colorPercentage >= thresholdPercentage) {
                return true
            }
        }

        return false
    }

    override fun checkIfMostBlackImageIsLockable(
        bitmap: Bitmap,
        onBlankLock: () -> Unit,
        onNotBlank: () -> Unit
    ) {
        ownerScope.launch(Dispatchers.IO) {
            val isBlank = hasDominantColor(bitmap)
            if(!isBlank) {
                withContext(Dispatchers.Main) {
                    onNotBlank()
                }
                return@launch
            }
            val getBlankCounter = sharedPreferences.getCurrentBlankImageCounter()
            sharedPreferences.setCurrentBlankImageCounter(getBlankCounter + 1)
            if(getBlankCounter >= BLANK_IMAGE_OCCURUANCE_THRESHOLD) {
                withContext(Dispatchers.Main) {
                    onBlankLock()
                }
                sharedPreferences.setCurrentBlankImageCounter(0)
            }
        }
    }
}
