package com.example.sihscrap.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.sihscrap.ui.SharedViewModel
import com.example.sihscrap.ui.ScannedItem
import com.example.sihscrap.ai.ClassificationResult
import com.example.sihscrap.ai.MaterialTier
import com.example.sihscrap.ai.YoloScrapClassifier
import com.example.sihscrap.ai.ThrottledImageAnalyzer
import com.example.sihscrap.api.MultimodalAnalysisResponse
import com.example.sihscrap.api.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.concurrent.Executors



data class CameraDeviceInfo(
    val id: String,
    val label: String,
    val cameraSelector: CameraSelector
)

@Composable
fun CameraScreen(navController: NavController, sharedViewModel: SharedViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val classifier = remember { YoloScrapClassifier(context) }
    var currentResult by remember {
        mutableStateOf(
            ClassificationResult(
                categoryCode = "no_detection",
                categoryName = "🔍 Point camera at scrap / appliance",
                confidence = 0.0f,
                rustPercentage = 0.0f,
                materialTier = MaterialTier.SLATE,
                priceDeductionPercentage = 0.0f
            )
        )
    }

    var backendError by remember { mutableStateOf<String?>(null) }
    var customBackendIp by remember { mutableStateOf(RetrofitClient.getBaseUrl()) }
    var showIpDialog by remember { mutableStateOf(false) }

    var isShutterLocked by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    var multimodalReport by remember { mutableStateOf<MultimodalAnalysisResponse?>(null) }
    var isAnalyzingMultimodal by remember { mutableStateOf(false) }


    val cameraOptions = remember { mutableStateListOf<CameraDeviceInfo>() }
    var selectedCameraOption by remember { mutableStateOf<CameraDeviceInfo?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val triggerBatchAudit: () -> Unit = {
        isShutterLocked = true
        isAnalyzingMultimodal = true
        scope.launch {
            try {
                val currentBmp = previewViewRef?.bitmap ?: Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
                val stream = java.io.ByteArrayOutputStream()
                currentBmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val realImageBytes = stream.toByteArray()
                val reqFile = RequestBody.create(MediaType.parse("image/jpeg"), realImageBytes)
                val body = MultipartBody.Part.createFormData("file", "${currentResult.categoryCode}.jpg", reqFile)
                val response = RetrofitClient.instance.analyzeMultimodal(body)
                multimodalReport = response
                backendError = null
            } catch (e: Exception) {
                multimodalReport = null
                backendError = "🔴 Backend VLM Server Unreachable (${e.javaClass.simpleName}):\n${e.localizedMessage ?: e.message}\n\nEndpoint: ${RetrofitClient.getBaseUrl()}/api/v1/vision/analyze-multimodal\n\nEnsure backend server is running on host laptop and mobile device is on the same local network."
            } finally {
                isAnalyzingMultimodal = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(selectedCameraOption, cameraProvider, previewViewRef) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val pView = previewViewRef ?: return@LaunchedEffect
        val option = selectedCameraOption ?: return@LaunchedEffect

        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        cameraExecutor,
                        ThrottledImageAnalyzer(classifier) { result ->
                            if (!isShutterLocked) {
                                currentResult = result
                            }
                        }
                    )
                }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                option.cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Preview + Throttled ImageAnalysis
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val discovered = mutableListOf<CameraDeviceInfo>()
                    try {
                        provider.availableCameraInfos.forEachIndexed { index, info ->
                            val lensFacing = try { info.lensFacing } catch (e: Exception) { -1 }
                            val label = when (lensFacing) {
                                CameraSelector.LENS_FACING_BACK -> if (index == 0) "Back Camera (Rear)" else "Back Camera #$index"
                                CameraSelector.LENS_FACING_FRONT -> if (index == 1 || index == 0) "Front Camera (Selfie)" else "Front Camera #$index"
                                CameraSelector.LENS_FACING_EXTERNAL -> "External Camera #$index"
                                else -> "Camera #$index"
                            }
                            val selector = try {
                                info.cameraSelector
                            } catch (e: Throwable) {
                                when (lensFacing) {
                                    CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            }
                            discovered.add(CameraDeviceInfo(id = "cam_$index", label = label, cameraSelector = selector))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (discovered.isEmpty()) {
                        if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            discovered.add(CameraDeviceInfo("back", "Back Camera (Rear)", CameraSelector.DEFAULT_BACK_CAMERA))
                        }
                        if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            discovered.add(CameraDeviceInfo("front", "Front Camera (Selfie)", CameraSelector.DEFAULT_FRONT_CAMERA))
                        }
                    }
                    if (discovered.isEmpty()) {
                        discovered.add(CameraDeviceInfo("default", "Default Camera", CameraSelector.DEFAULT_BACK_CAMERA))
                    }

                    cameraOptions.clear()
                    cameraOptions.addAll(discovered)

                    if (selectedCameraOption == null) {
                        selectedCameraOption = discovered.firstOrNull { it.label.contains("Back", ignoreCase = true) }
                            ?: discovered.firstOrNull()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // HUD High-Contrast Targeting Box
        val tierColor = Color(currentResult.materialTier.colorHex)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val boxLeft = width * 0.15f
            val boxTop = height * 0.25f
            val boxWidth = width * 0.70f
            val boxHeight = height * 0.40f

            // Bounding box border
            drawRect(
                color = tierColor,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = 6f)
            )

            // Corner brackets
            val bracketLen = 36f
            val bracketStroke = 12f
            // Top-Left
            drawLine(tierColor, Offset(boxLeft, boxTop), Offset(boxLeft + bracketLen, boxTop), strokeWidth = bracketStroke)
            drawLine(tierColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + bracketLen), strokeWidth = bracketStroke)
            // Top-Right
            drawLine(tierColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth - bracketLen, boxTop), strokeWidth = bracketStroke)
            drawLine(tierColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth, boxTop + bracketLen), strokeWidth = bracketStroke)
            // Bottom-Left
            drawLine(tierColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft + bracketLen, boxTop + boxHeight), strokeWidth = bracketStroke)
            drawLine(tierColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft, boxTop + boxHeight - bracketLen), strokeWidth = bracketStroke)
            // Bottom-Right
            drawLine(tierColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth - bracketLen, boxTop + boxHeight), strokeWidth = bracketStroke)
            drawLine(tierColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth, boxTop + boxHeight - bracketLen), strokeWidth = bracketStroke)
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Camera Selection Dropdown Menu
            Box {
                Surface(
                    onClick = { isDropdownExpanded = !isDropdownExpanded },
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Select Camera",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedCameraOption?.label ?: "Select Camera",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Menu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .widthIn(min = 200.dp)
                ) {
                    if (cameraOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Detecting cameras...", fontSize = 13.sp) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        cameraOptions.forEach { option ->
                            val isSelected = option.id == selectedCameraOption?.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedCameraOption = option
                                    isDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Material Tier Badge
            Surface(
                color = tierColor,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = currentResult.materialTier.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // AI Engine Status & Host IP Pill Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 96.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (classifier.isOperational) Color(0xFF1B5E20).copy(alpha = 0.85f) else Color(0xFFB71C1C).copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (classifier.isOperational) Color(0xFF4CAF50) else Color(0xFFEF5350))
            ) {
                Text(
                    text = if (classifier.isOperational) "🟢 ${classifier.engineStatus}" else "🔴 ${classifier.engineStatus}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }

            Surface(
                onClick = { showIpDialog = true },
                color = Color.Black.copy(alpha = 0.70f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "🌐 Host: ${RetrofitClient.getBaseUrl().replace("http://", "")}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }

        // Bottom Inspection & Classification Overlay Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val isNoDetection = currentResult.categoryCode == "no_detection"
                            val isEngineError = currentResult.categoryCode == "engine_error"

                            Text(
                                text = currentResult.categoryName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isEngineError) Color(0xFFD50000) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    isEngineError -> "Neural engine error | Inspect device logs"
                                    isNoDetection -> "Targeting: Align mouse, phone, laptop, fan, AC, or scrap in box"
                                    else -> "Confidence: ${(currentResult.confidence * 100).toInt()}% | Dual YOLO Neural Engine"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isShutterLocked) {
                            Surface(color = Color(0xFF00C853), shape = CircleShape) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rust / Oxidation Percentage Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rust / Oxidation: ${currentResult.rustPercentage.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "-${(currentResult.priceDeductionPercentage * 100).toInt()}% Price Penalty",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentResult.rustPercentage > 20f) Color(0xFFD50000) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = (currentResult.rustPercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = if (currentResult.rustPercentage > 25f) Color(0xFFD50000) else Color(0xFFFFB300),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Multi-item Cart Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isNoDetection = currentResult.categoryCode == "no_detection"
                        val isEngineError = currentResult.categoryCode == "engine_error"

                        Button(
                            onClick = {
                                triggerBatchAudit()
                            },
                            enabled = !isAnalyzingMultimodal && !isEngineError,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNoDetection) Color.Gray else tierColor
                            )
                        ) {
                            if (isAnalyzingMultimodal) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditing with 7B VLM...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Camera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isNoDetection) "Audit Viewfinder" else "Add to Batch",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (sharedViewModel.cart.isNotEmpty()) {
                            Button(
                                onClick = {
                                    navController.navigate("calculator")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Review (${sharedViewModel.cart.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Multimodal Hazard & Condition Assessment Dialog (Triggered on Add to Batch)
        multimodalReport?.let { report ->
            AlertDialog(
                onDismissRequest = {
                    multimodalReport = null
                    isShutterLocked = false
                },
                title = {
                    Column {
                        Text(
                            text = "🔬 ${report.itemName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${report.itemNameHi} | ${report.itemNameMr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        // Hazard Alert Banner
                        Surface(
                            color = if (report.safetyHazard.hasToxicHazards) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (report.safetyHazard.hasToxicHazards) Color(0xFFD32F2F) else Color(0xFF388E3C)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (report.safetyHazard.hasToxicHazards) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hazard Level: ${report.safetyHazard.hazardLevel}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (report.safetyHazard.hasToxicHazards) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = report.safetyHazard.alertEn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "🇮🇳 ${report.safetyHazard.alertHi}", fontSize = 11.sp, color = Color(0xFF424242))
                                Text(text = "🚩 ${report.safetyHazard.alertMr}", fontSize = 11.sp, color = Color(0xFF424242))
                                if (report.safetyHazard.toxicSubstances.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Toxic Elements: " + report.safetyHazard.toxicSubstances.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = Color(0xFFC62828),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Condition Assessment
                        Text(text = "Condition Assessment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Wear: ${report.condition.wearGrade}", fontSize = 12.sp)
                        Text(text = "• Casing Intactness: ${report.condition.casingIntactnessPct}%", fontSize = 12.sp)
                        Text(text = "• Surface Oxidation: ${report.condition.oxidationRustPct}%", fontSize = 12.sp)
                        Text(text = "• Purity Multiplier: ${(report.condition.purityFactor * 100).toInt()}%", fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Valuation Quote
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Fair Mandi Valuation (CPCB Grounded)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Base Spot Rate: ₹${report.valuation.baseMandiRateInrPerKg}/kg",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val minP = report.valuation.estimatedPayoutRangeInr.firstOrNull() ?: 0.0
                                val maxP = report.valuation.estimatedPayoutRangeInr.lastOrNull() ?: 0.0
                                Text(
                                    text = "Est. Payout: ₹$minP - ₹$maxP",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF00C853)
                                )
                                Text(
                                    text = "🌱 Avoided CO₂: ${report.valuation.carbonOffsetKg} kg",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🧠 Model: ${report.aiEngine}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sharedViewModel.addToCart(
                                ScannedItem(
                                    code = report.valuation.materialCode,
                                    name = report.itemName,
                                    rust = report.condition.oxidationRustPct.toFloat(),
                                    weight = 0.0
                                )
                            )
                            multimodalReport = null
                            isShutterLocked = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            multimodalReport = null
                            isShutterLocked = false
                        }
                    ) {
                        Text("Re-scan", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Explicit Backend Connection / VLM Error Alert Dialog
        backendError?.let { err ->
            AlertDialog(
                onDismissRequest = {
                    backendError = null
                    isShutterLocked = false
                },
                icon = {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                title = {
                    Text("⚠️ Multimodal AI Server Error", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = err,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Verify or update host laptop backend address:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customBackendIp,
                            onValueChange = { customBackendIp = it },
                            label = { Text("Backend URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            RetrofitClient.setBaseUrl(customBackendIp)
                            backendError = null
                            triggerBatchAudit()
                        }
                    ) {
                        Text("Update & Retry")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            backendError = null
                            isShutterLocked = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Manual Host IP Configuration Dialog
        if (showIpDialog) {
            var tempIp by remember { mutableStateOf(RetrofitClient.getBaseUrl()) }
            AlertDialog(
                onDismissRequest = { showIpDialog = false },
                title = { Text("Configure Backend VLM Server IP", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Enter the URL of the laptop running the Qwen2.5-VL backend (e.g., http://192.168.88.8:8000):",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = tempIp,
                            onValueChange = { tempIp = it },
                            label = { Text("Server URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            RetrofitClient.setBaseUrl(tempIp)
                            customBackendIp = tempIp
                            showIpDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showIpDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

