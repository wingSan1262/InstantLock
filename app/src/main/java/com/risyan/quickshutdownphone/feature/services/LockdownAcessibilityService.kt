package com.risyan.quickshutdownphone.feature.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.risyan.quickshutdownphone.MyApp
import com.risyan.quickshutdownphone.feature.checkJobAndSaveLockStatus
import com.risyan.quickshutdownphone.feature.hasStorageAccess
import com.risyan.quickshutdownphone.feature.receivers.ShutdownType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class LockdownAcessibilityService : AccessibilityService() {

    val scope = CoroutineScope(Dispatchers.IO)
    val aiNsfwGrader = AiNsfwGraderImp(this, scope)
    val screenShotService = ScreenShotServiceImp(this, scope)
    val sharedPrefApi = MyApp.getInstance().sharedPrefApi

    var periodicScreenShotJob : Job? = null

    val SCREENSHOT_INTERVAL = 20000L

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        initPeriodicScreenShot()
    }

    fun initPeriodicScreenShot(){
        periodicScreenShotJob?.cancel();
        periodicScreenShotJob = scope.launch {
            delay(250L)
            if(!hasStorageAccess() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                delay(SCREENSHOT_INTERVAL)
                initPeriodicScreenShot()
                return@launch
            }
            doScreenShot()
            delay(SCREENSHOT_INTERVAL)
            initPeriodicScreenShot()
        }
    }

    fun doScreenShot(){
        screenShotService.takeScreenShot { bitmap ->
            aiNsfwGrader.checkIfNsfw(bitmap) { isNsfw ->
                scope.launch(Dispatchers.Main){
                    if(isNsfw){
//                    checkJobAndSaveLockStatus(ShutdownType.QUICK_5_MINUTES, sharedPrefApi)
                        Toast.makeText(this@LockdownAcessibilityService, "NSFW Detected", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this@LockdownAcessibilityService, "SFW Detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}


interface AiNsfwGrader{
    fun checkIfNsfw(
        bitmap: Bitmap,
        onResult: (Boolean) -> Unit
    )
}

class AiNsfwGraderImp(
    val owner: LockdownAcessibilityService,
    val ownerScope: CoroutineScope
) : AiNsfwGrader {

    private val tfliteInterpreter: Interpreter by lazy {
        Interpreter(loadModelFile()) // Use the Interpreter class to create an instance
    }

    fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = owner.assets.openFd("nsfw.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private val INPUT_WIDTH = 224
    private val INPUT_HEIGHT = 224
    private val VGG_MEAN = floatArrayOf(103.939f, 116.779f, 123.68f)
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        ByteBuffer.allocateDirect(1 * INPUT_WIDTH * INPUT_HEIGHT * 3 * 4).also { imgData ->

            imgData.order(ByteOrder.LITTLE_ENDIAN)

            SystemClock.uptimeMillis().let { startTime ->
                imgData.rewind()
                IntArray(INPUT_WIDTH * INPUT_HEIGHT).let {
                    //把每个像素的颜色值转为int 存入intValues
                    bitmap.getPixels(
                        it,
                        0,
                        INPUT_WIDTH,
                        Math.max((bitmap.height - INPUT_HEIGHT) / 2, 0),
                        Math.max((bitmap.width - INPUT_WIDTH) / 2, 0),
                        INPUT_WIDTH,
                        INPUT_HEIGHT
                    )
                    for (color in it) {
                        imgData.putFloat(Color.blue(color) - VGG_MEAN[0])
                        imgData.putFloat(Color.green(color) - VGG_MEAN[1])
                        imgData.putFloat(Color.red(color) - VGG_MEAN[2])
                    }
                }
                return imgData
            }
        }
    }

    fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224 // Change to your model's input size
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 3 channels (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }

    fun gradeImage(bitmap: Bitmap): FloatArray {
        // Update OUTPUT_CLASSES to match your model's output (2 in this case)
        val OUTPUT_CLASSES = 2
        val output = Array(1) { FloatArray(OUTPUT_CLASSES) }
        val input = bitmapToByteBuffer(bitmap)
        tfliteInterpreter.run(input, output)

        // Log output for debugging
        for (i in 0 until OUTPUT_CLASSES) {
            Log.d("Model Output", "Class $i Score: ${output[0][i]}")
        }

        return output[0] // Return the output array for further processing
    }

    override fun checkIfNsfw(
        bitmap: Bitmap,
        onResult: (Boolean) -> Unit
    ) {
        ownerScope.launch(Dispatchers.IO) {
            // Check if image is NSFW
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            val outputScores = gradeImage(scaledBitmap)

            // Assuming index 0 is NSFW and index 1 is SFW
            val sfwScore = outputScores[0]
            val nsfwScore = outputScores[1]

            // You can modify this logic as per your requirement
            val isNsfw = nsfwScore > sfwScore

            withContext(Dispatchers.Main) {
                onResult(isNsfw)
            }
        }
    }
}






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
                        ownerScope.launch(Dispatchers.Main){
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
