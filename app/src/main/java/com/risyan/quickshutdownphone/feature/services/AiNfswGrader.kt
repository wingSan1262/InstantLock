package com.risyan.quickshutdownphone.feature.services

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import com.risyan.quickshutdownphone.feature.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min


interface AiNsfwGrader {
    fun checkIfNsfw(
        bitmap: Bitmap,
        onResult: (Boolean, Boolean) -> Unit
    )
}

class NSFWSexyImagePreProcessing() {
    private val INPUT_WIDTH = 224
    private val INPUT_HEIGHT = 224
    private val VGG_MEAN = floatArrayOf(103.939f, 116.779f, 123.68f)

    fun bitmapToByteBufferImageTensor(tflite: Interpreter, bitmap: Bitmap): ByteBuffer {
        val imageType = tflite.getInputTensor(0).dataType()
        val inputImageBuffer = TensorImage(imageType)
        inputImageBuffer.load(bitmap)

        val IMAGE_MEAN = 0f;
        val IMAGE_STD = 255f;

        // Creates processor for the TensorImage.
        val cropSize: Int = Math.min(bitmap.getWidth(), bitmap.getHeight())
        val numRoration: Int = 0 / 90
        val imageShape = tflite.getInputTensor(0).shape() // {1, height, width, 3}
        val imageSizeY = imageShape[1]
        val imageSizeX = imageShape[2]


        val imageProcessor: ImageProcessor =
            ImageProcessor.Builder()
                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
                .add(Rot90Op(numRoration))
                .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build()
        return imageProcessor.process(inputImageBuffer).buffer
    }

    fun bitmapToByteBufferRawProcessing(bitmap: Bitmap): ByteBuffer {
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

    fun cropCenterSquare(bitmap: Bitmap): Bitmap {
        val dimension = min(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - dimension) / 2
        val yOffset = (bitmap.height - dimension) / 2
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, dimension, dimension)
    }
}

class AiNsfwGraderImp(
    val owner: LockdownAcessibilityService,
    val ownerScope: CoroutineScope,
    val nsfwImagePreprocessor: NSFWSexyImagePreProcessing = NSFWSexyImagePreProcessing()
) : AiNsfwGrader {


    private val NSFW_FILE = "nsfw.tflite"
    private val SEXY_NSFW_FILE = "sexy_nsfw.tflite"

    private val OUTPUT_FILE_NFSW_COUNT = 2;
    private val OUTPUT_FILE_SEXY_NFSW_COUNT = 4;

    private val NFSW_THRESHOLD = 0.75
    private val NFSW_SEXY_THRESHOLD = 0.4
    private val SEXY_THRESHOLD = 0.5

    private val nsfwInterpreter: Interpreter by lazy {
        Interpreter(loadNsfwModel(NSFW_FILE)) // Use the Interpreter class to create an instance
    }
    private val sexyNsfwInterpreter: Interpreter by lazy {
        Interpreter(loadNsfwModel(SEXY_NSFW_FILE)) // Use the Interpreter class to create an instance
    }

    fun loadNsfwModel(
        fileName: String
    ): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = owner.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun gradeImage(
        interpreter: Interpreter,
        input: ByteBuffer,
        outputClasses: Int = OUTPUT_FILE_NFSW_COUNT
    ): FloatArray {
        val output = Array(1) { FloatArray(outputClasses) }
        try {
            interpreter.run(input, output)
        } catch (e: Exception) {
            owner.showToast("Please contact dev, Error grading : ${e.message}")
        }
        return output[0] // Return the output array for further processing
    }

    override fun checkIfNsfw(
        bitmap: Bitmap,
        onResult: (isNsfw: Boolean, isSexy: Boolean) -> Unit
    ) {
        ownerScope.launch(Dispatchers.IO) {
            var scaledBitmap = nsfwImagePreprocessor.cropCenterSquare(bitmap)
            scaledBitmap = Bitmap.createScaledBitmap(
                scaledBitmap, 256, 256, true
            )
            val nsfwOutputReading = gradeImage(
                nsfwInterpreter,
                nsfwImagePreprocessor.bitmapToByteBufferRawProcessing(scaledBitmap),
                OUTPUT_FILE_NFSW_COUNT
            )
            val nsfwScore = nsfwOutputReading[1]

            val sexyOutputNsfw = gradeImage(
                sexyNsfwInterpreter,
                nsfwImagePreprocessor.bitmapToByteBufferImageTensor(
                    sexyNsfwInterpreter,
                    scaledBitmap
                ),
                OUTPUT_FILE_SEXY_NFSW_COUNT
            )
            val sexyScore = sexyOutputNsfw[3]
            withContext(Dispatchers.Main) {
                onResult(
                    nsfwScore > NFSW_THRESHOLD,
                    sexyScore > SEXY_THRESHOLD || nsfwScore > NFSW_SEXY_THRESHOLD
                )
            }
        }
    }
}
