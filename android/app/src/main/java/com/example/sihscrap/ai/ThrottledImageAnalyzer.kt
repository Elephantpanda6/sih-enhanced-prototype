package com.example.sihscrap.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ThrottledImageAnalyzer(
    private val classifier: YoloScrapClassifier,
    private val onResult: (ClassificationResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val TAG = "ThrottledAnalyzer"
    private var lastAnalyzedTimestamp: Long = 0L
    private val THROTTLE_INTERVAL_MS: Long = 180L // ~5.5 FPS responsive, high-fidelity frame sampling

    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < THROTTLE_INTERVAL_MS) {
            // Strictly dispose imageProxy immediately
            imageProxy.close()
            return
        }

        try {
            lastAnalyzedTimestamp = currentTimestamp
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                val result = classifier.analyzeBitmap(bitmap)
                onResult(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during throttled frame analysis: ${e.message}", e)
        } finally {
            // Crucial: explicit close in finally block
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val bmp = imageProxy.toBitmap()
            Bitmap.createScaledBitmap(bmp, 224, 224, true)
        } catch (e: Exception) {
            try {
                // High-performance fallback for direct buffer reading
                val planes = imageProxy.planes
                if (planes.isNotEmpty()) {
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * imageProxy.width
                    val bitmap = Bitmap.createBitmap(
                        imageProxy.width + rowPadding / pixelStride,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    Bitmap.createScaledBitmap(bitmap, 224, 224, true)
                } else null
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap: ${e.message}")
                null
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
