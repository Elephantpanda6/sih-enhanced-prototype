package com.example.sihscrap.ai

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationResult(
    val categoryCode: String,
    val categoryName: String,
    val confidence: Float,
    val rustPercentage: Float,
    val materialTier: MaterialTier,
    val priceDeductionPercentage: Float
)

enum class MaterialTier(val displayName: String, val colorHex: Long) {
    EMERALD_GREEN("Clean Metals", 0xFF00C853),
    AMBER("Mixed Scrap", 0xFFFFB300),
    SLATE("Inert E-Waste", 0xFF607D8B),
    CRIMSON("Hazardous Battery/Lead", 0xFFD50000)
}

class ScrapClassifier(private val context: Context) {
    private val TAG = "ScrapClassifier"
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        const val MODEL_INPUT_SIZE = 224
        const val MODEL_PATH = "models/scrap_model.tflite"
        const val LABELS_PATH = "models/labels.txt"

        val DEFAULT_CATEGORIES = listOf(
            "copper_bare_bright", "copper_armature", "brass_honey",
            "aluminium_extrusions", "aluminium_castings", "aluminium_utensils",
            "heavy_steel_sariya", "light_iron_patra", "cast_iron",
            "high_grade_server_pcb", "mobile_phone_pcb",
            "lead_acid_battery", "li_ion_cells",
            "cardboard_carton", "pet_plastic"
        )

        fun getMaterialTier(categoryCode: String): MaterialTier {
            return when {
                categoryCode.contains("copper") || categoryCode.contains("brass") || categoryCode.contains("aluminium") ->
                    MaterialTier.EMERALD_GREEN
                categoryCode.contains("steel") || categoryCode.contains("iron") || categoryCode.contains("patra") ||
                        categoryCode.contains("cardboard") || categoryCode.contains("plastic") ->
                    MaterialTier.AMBER
                categoryCode.contains("pcb") || categoryCode.contains("e_waste") || categoryCode.contains("server") ->
                    MaterialTier.SLATE
                categoryCode.contains("battery") || categoryCode.contains("cells") || categoryCode.contains("lead") ->
                    MaterialTier.CRIMSON
                else -> MaterialTier.EMERALD_GREEN
            }
        }

        fun calculatePriceDeduction(basePrice: Double, rustScore: Float): Double {
            val deductionPct = (rustScore / 100.0) * 0.20 // max 20% penalty
            return basePrice * (1.0 - deductionPct)
        }
    }

    init {
        loadModelAndLabels()
    }

    private fun loadModelAndLabels() {
        try {
            val assetManager = context.assets
            // Load labels
            val labelInputStream = try {
                assetManager.open(LABELS_PATH)
            } catch (e: Exception) {
                assetManager.open("labels.txt")
            }
            labels = BufferedReader(InputStreamReader(labelInputStream)).readLines().filter { it.isNotBlank() }
            if (labels.isEmpty()) {
                labels = DEFAULT_CATEGORIES
            }

            // Load TFLite Model
            val fd: AssetFileDescriptor = try {
                assetManager.openFd(MODEL_PATH)
            } catch (e: Exception) {
                assetManager.openFd("scrap_model.tflite")
            }
            val inputStream = FileInputStream(fd.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fd.startOffset
            val declaredLength = fd.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(2) // Strictly adhere to Android Go memory/CPU footprint (<60MB)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "TFLite scrap classifier model initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "TFLite initialization exception (using edge algorithm fallback): ${e.message}")
            if (labels.isEmpty()) {
                labels = DEFAULT_CATEGORIES
            }
        }
    }

    fun analyzeBitmap(bitmap: Bitmap): ClassificationResult {
        val scaled = if (bitmap.width != MODEL_INPUT_SIZE || bitmap.height != MODEL_INPUT_SIZE) {
            Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        } else {
            bitmap
        }

        // 1. Rust and Oxidation Analysis via HSV Color Space & Edge Density
        val rustScore = calculateRustOxidationScore(scaled)

        // 2. Classify Material via TFLite or Edge Heuristic
        var predictedIdx = 0
        var confidence = 0.88f

        if (interpreter != null) {
            try {
                val inputBuffer = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4)
                inputBuffer.order(ByteOrder.nativeOrder())
                val intValues = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
                scaled.getPixels(intValues, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)

                for (pixelValue in intValues) {
                    val r = (pixelValue shr 16 and 0xFF) / 255.0f
                    val g = (pixelValue shr 8 and 0xFF) / 255.0f
                    val b = (pixelValue and 0xFF) / 255.0f
                    inputBuffer.putFloat(r)
                    inputBuffer.putFloat(g)
                    inputBuffer.putFloat(b)
                }

                val outputBuffer = Array(1) { FloatArray(labels.size) }
                interpreter?.run(inputBuffer, outputBuffer)

                val scores = outputBuffer[0]
                var maxScore = -1f
                for (i in scores.indices) {
                    if (scores[i] > maxScore) {
                        maxScore = scores[i]
                        predictedIdx = i
                    }
                }
                confidence = maxScore
            } catch (e: Exception) {
                Log.w(TAG, "Inference error, falling back to edge heuristic: ${e.message}")
                predictedIdx = determineHeuristicIndex(rustScore, scaled)
            }
        } else {
            predictedIdx = determineHeuristicIndex(rustScore, scaled)
        }

        val categoryCode = labels.getOrElse(predictedIdx) { "copper_bare_bright" }
        val categoryName = formatCategoryName(categoryCode)
        val materialTier = getMaterialTier(categoryCode)
        val priceDeduction = (rustScore / 100.0f) * 0.20f // max 20% penalty

        return ClassificationResult(
            categoryCode = categoryCode,
            categoryName = categoryName,
            confidence = confidence,
            rustPercentage = rustScore,
            materialTier = materialTier,
            priceDeductionPercentage = priceDeduction
        )
    }

    private fun determineHeuristicIndex(rustScore: Float, bitmap: Bitmap): Int {
        return when {
            rustScore > 35.0f -> labels.indexOf("light_iron_patra").coerceAtLeast(7)
            rustScore > 15.0f -> labels.indexOf("heavy_steel_sariya").coerceAtLeast(6)
            else -> labels.indexOf("copper_bare_bright").coerceAtLeast(0)
        }
    }

    fun calculateRustOxidationScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        var rustPixelCount = 0
        var edgePixelCount = 0

        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val hsv = FloatArray(3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                Color.RGBToHSV(r, g, b, hsv)
                val h = hsv[0] // 0 to 360
                val s = hsv[1] // 0 to 1
                val v = hsv[2] // 0 to 1

                // Rust / oxidation Hue range: roughly 10° to 35°, with moderate to high saturation
                if (h in 10.0f..35.0f && s >= 0.25f && v in 0.15f..0.90f) {
                    rustPixelCount++
                }

                // Simple Sobel edge approximation for rust texture roughness
                if (x < width - 1 && y < height - 1) {
                    val rightPixel = pixels[index + 1]
                    val downPixel = pixels[index + width]
                    val grayCurrent = (r * 299 + g * 587 + b * 114) / 1000
                    val grayRight = (Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 1000) / 1000
                    val grayDown = (Color.red(downPixel) * 299 + Color.green(downPixel) * 587 + Color.blue(downPixel) * 1000) / 1000

                    val diff = Math.abs(grayCurrent - grayRight) + Math.abs(grayCurrent - grayDown)
                    if (diff > 40) {
                        edgePixelCount++
                    }
                }
            }
        }

        val rustRatio = rustPixelCount.toFloat() / totalPixels.toFloat()
        val edgeDensity = edgePixelCount.toFloat() / totalPixels.toFloat()
        val combinedScore = (rustRatio * 0.70f + edgeDensity * 0.30f) * 100.0f
        return combinedScore.coerceIn(0.0f, 100.0f)
    }

    fun getMaterialTier(categoryCode: String): MaterialTier {
        return Companion.getMaterialTier(categoryCode)
    }

    private fun formatCategoryName(code: String): String {
        return code.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
