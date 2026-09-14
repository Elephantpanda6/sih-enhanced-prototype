package com.example.sihscrap.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.util.Collections

class YoloScrapClassifier(private val context: Context) {
    private val TAG = "YoloScrapClassifier"
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    
    private val INPUT_SIZE = 224
    private val NUM_CLASSES = 15
    private val NUM_ANCHORS = 1029

    private val LABELS = arrayOf(
        "copper_bare_bright", "copper_armature", "brass_honey", "aluminium_extrusions",
        "aluminium_castings", "aluminium_utensils", "heavy_steel_sariya", "light_iron_patra",
        "cast_iron", "high_grade_server_pcb", "mobile_phone_pcb", "lead_acid_battery",
        "li_ion_cells", "cardboard_carton", "pet_plastic"
    )

    init {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val assetManager = context.assets
            val modelBytes = assetManager.open("models/scrap_seg_model.onnx").readBytes()
            
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            ortSession = ortEnv?.createSession(modelBytes, options)
            Log.d(TAG, "YOLOv8 ONNX model initialized successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "YOLO model missing. Drop best.onnx into assets/models/!")
        }
    }

    fun analyzeBitmap(bitmap: Bitmap): ClassificationResult {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // 1. Prepare ONNX Input Tensor (1, 3, 224, 224) - CHW Format RGB
        val inputBuffer = FloatBuffer.allocate(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        val channelSize = INPUT_SIZE * INPUT_SIZE
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255.0f
            val g = ((p shr 8) and 0xFF) / 255.0f
            val b = (p and 0xFF) / 255.0f
            
            inputBuffer.put(i, r) // R
            inputBuffer.put(i + channelSize, g) // G
            inputBuffer.put(i + 2 * channelSize, b) // B
        }
        
        val inputName = ortSession?.inputNames?.iterator()?.next() ?: "images"
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        
        var bestScore = 0f
        var bestClassIndex = -1

        try {
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, shape)
            
            // 2. Run Inference
            val result = ortSession?.run(Collections.singletonMap(inputName, inputTensor))
            
            // 3. Parse Detections [1, 31, 1029]
            val output = result?.get(0)?.value as Array<Array<FloatArray>>
            val predictions = output[0] // shape: [31][1029]
            
            for (anchor in 0 until NUM_ANCHORS) {
                for (c in 0 until NUM_CLASSES) {
                    val score = predictions[4 + c][anchor]
                    if (score > bestScore) {
                        bestScore = score
                        bestClassIndex = c
                    }
                }
            }
            
            result.close()
            inputTensor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
        }

        // Return empty if nothing is confident
        if (bestScore < 0.25f || bestClassIndex == -1) {
            return ClassificationResult(
                categoryCode = "none",
                categoryName = "Scanning...",
                confidence = 0f,
                rustPercentage = 0f,
                materialTier = MaterialTier.SLATE,
                priceDeductionPercentage = 0f
            )
        }

        val detectedName = LABELS[bestClassIndex]
        return ClassificationResult(
            categoryCode = detectedName,
            categoryName = detectedName.replace("_", " ").split(" ").joinToString(" ") { it.capitalize() },
            confidence = bestScore,
            rustPercentage = 15.5f, // Mask rust placeholder
            materialTier = MaterialTier.EMERALD_GREEN,
            priceDeductionPercentage = 0.15f
        )
    }

    fun close() {
        ortSession?.close()
        ortEnv?.close()
    }
}
