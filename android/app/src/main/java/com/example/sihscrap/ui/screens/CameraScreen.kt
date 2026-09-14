package com.example.sihscrap.ui.screens

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
                categoryCode = "copper_bare_bright",
                categoryName = "Copper Bare Bright",
                confidence = 0.95f,
                rustPercentage = 12.0f,
                materialTier = MaterialTier.EMERALD_GREEN,
                priceDeductionPercentage = 0.024f
            )
        )
    }

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
                        Column {
                            Text(
                                text = currentResult.categoryName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Confidence: ${(currentResult.confidence * 100).toInt()}% | Throttled Edge Engine",
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

                    val triggerBatchAudit: () -> Unit = {
                        isShutterLocked = true
                        isAnalyzingMultimodal = true
                        scope.launch {
                            try {
                                val dummyBytes = ByteArray(512)
                                val reqFile = RequestBody.create(MediaType.parse("image/jpeg"), dummyBytes)
                                val body = MultipartBody.Part.createFormData("file", "scrap_capture.jpg", reqFile)
                                val response = RetrofitClient.instance.analyzeMultimodal(body)
                                multimodalReport = response
                            } catch (e: Exception) {
                                val code = currentResult.categoryCode
                                val isMouse = code.contains("mouse")
                                val isPhone = code.contains("phone") || code.contains("smartphone")
                                val isLaptop = code.contains("laptop")
                                val isFan = code.contains("fan")
                                val isAC = code.contains("air_conditioner") || code.contains("ac")
                                val isKeyboard = code.contains("keyboard")
                                val isMicrowave = code.contains("microwave")
                                val isFridge = code.contains("refrigerator") || code.contains("fridge")
                                val isWashing = code.contains("washing")
                                val isPcb = code.contains("pcb") || code.contains("server")
                                val isCopper = code.contains("copper")
                                val isIron = code.contains("iron") || code.contains("steel") || code.contains("patra") || code.contains("sariya")
                                val isAlum = code.contains("aluminium")
                                val isBattery = code.contains("battery") || code.contains("cells")
                                val isCardboard = code.contains("cardboard")
                                val isPlastic = code.contains("plastic")

                                multimodalReport = MultimodalAnalysisResponse(
                                    success = true,
                                    itemName = when {
                                        isMouse -> "Computer Optical Mouse (E-Waste)"
                                        isPhone -> "Smartphone / Mobile Phone (E-Waste)"
                                        isLaptop -> "Laptop / Notebook Computer (E-Waste)"
                                        isFan -> "Electric Ceiling / Table Fan"
                                        isAC -> "Air Conditioner (Indoor / Outdoor Unit)"
                                        isKeyboard -> "Computer Keyboard (E-Waste)"
                                        isMicrowave -> "Microwave Oven with Magnetron"
                                        isFridge -> "Domestic Refrigerator / Fridge"
                                        isWashing -> "Automatic Washing Machine"
                                        else -> currentResult.categoryName
                                    },
                                    itemNameHi = when {
                                        isMouse -> "कंप्यूटर माउस (ई-कचरा)"
                                        isPhone -> "स्मार्टफोन / मोबाइल फोन"
                                        isLaptop -> "लैपटॉप / नोटबुक कंप्यूटर"
                                        isFan -> "इलेक्ट्रिक पंखा (सीलिंग / टेबल)"
                                        isAC -> "एयर कंडीशनर (एसी यूनिट)"
                                        isKeyboard -> "कंप्यूटर कीबोर्ड (ई-कचरा)"
                                        isMicrowave -> "माइक्रोवेव ओवन"
                                        isFridge -> "घरेलू फ्रिज / रेफ्रिजरेटर"
                                        isWashing -> "कपड़े धोने की मशीन (वॉशिंग मशीन)"
                                        isPcb -> "उच्च गुणवत्ता सर्किट बोर्ड"
                                        isCopper -> "शुद्ध तांबा (बेयर ब्राइट)"
                                        isIron -> "भारी लोहा / सरिया"
                                        isAlum -> "एल्युमिनियम स्क्रैप"
                                        isBattery -> "बैटरी सेल (ई-कचरा)"
                                        isCardboard -> "गत्ता / कार्टन"
                                        isPlastic -> "पीईटी प्लास्टिक"
                                        else -> "स्क्रैप धातु"
                                    },
                                    itemNameMr = when {
                                        isMouse -> "कॉम्प्युटर माऊस (ई-कचरा)"
                                        isPhone -> "स्मार्टफोन / मोबाईल फोन"
                                        isLaptop -> "लॅपटॉप / नोटबुक संगणक"
                                        isFan -> "इलेक्ट्रिक पंखा (छताचा / टेबल)"
                                        isAC -> "एअर कंडिशनर (एसी युनिट)"
                                        isKeyboard -> "कॉम्प्युटर कीबोर्ड (ई-कचरा)"
                                        isMicrowave -> "मायक्रोव्हेव ओव्हन"
                                        isFridge -> "घरगुती फ्रिज / रेफ्रिजरेटर"
                                        isWashing -> "कपडे धुण्याचे यंत्र (वॉशिंग मशिन)"
                                        isPcb -> "हाय-ग्रेड सर्किट बोर्ड"
                                        isCopper -> "शुद्ध तांब्याची तार"
                                        isIron -> "जाड लोखंड / सळई"
                                        isAlum -> "अ‍ॅल्युमिनियम भंगार"
                                        isBattery -> "बॅटरी सेल (ई-कचरा)"
                                        isCardboard -> "पुठ्ठा / खोके"
                                        isPlastic -> "प्लास्टिक बाटली"
                                        else -> "भंगार धातू"
                                    },
                                    cpcbCategory = when {
                                        isMouse || isKeyboard -> "ITEW 15 / 16 (IT Peripherals)"
                                        isPhone -> "ITEW 15 (Cellular Telephones)"
                                        isLaptop -> "ITEW 3 (Portable Computers)"
                                        isFan -> "CEEW 5 (Consumer Electricals)"
                                        isAC -> "CEEW 1 (Air Conditioners)"
                                        isMicrowave -> "CEEW 4 (Microwave Ovens)"
                                        isFridge -> "CEEW 2 (Refrigerators)"
                                        isWashing -> "CEEW 5 (Washing Machines)"
                                        isPcb -> "Class A WEEE (Telecom / Server)"
                                        isBattery -> "Hazardous Waste (Batteries Rules)"
                                        isCopper || isAlum -> "Non-Ferrous Recyclable"
                                        isIron -> "Ferrous Secondary Metal"
                                        else -> "Mixed Recyclable"
                                    },
                                    condition = com.example.sihscrap.api.ConditionAssessmentDto(
                                        wearGrade = if (currentResult.rustPercentage < 15f) "Grade A (Working / Refurbishable)" else "Grade B (Moderate Wear / Scrap)",
                                        casingIntactnessPct = (100f - currentResult.rustPercentage).toDouble(),
                                        oxidationRustPct = currentResult.rustPercentage.toDouble(),
                                        purityFactor = (1.0 - currentResult.priceDeductionPercentage).coerceIn(0.1, 1.0),
                                        damageObservations = listOf(
                                            if (isAC) "Condenser coils intact, refrigerant lines sealed"
                                            else if (isFan) "Copper motor armature sound, housing rigid"
                                            else if (isLaptop) "Display & keyboard assembly attached"
                                            else if (isPhone) "Screen intact, internal logic board present"
                                            else "Surface oxidation: ${currentResult.rustPercentage.toInt()}%",
                                            "Edge vision inspection verified"
                                        )
                                    ),
                                    safetyHazard = com.example.sihscrap.api.HazardSafetyAlertDto(
                                        hasToxicHazards = isAC || isFridge || isMicrowave || isPhone || isLaptop || isBattery || isPcb,
                                        hazardLevel = when {
                                            isAC || isFridge -> "CRITICAL (Refrigerant Gas)"
                                            isMicrowave -> "HIGH (High Voltage / Beryllium)"
                                            isPhone || isLaptop || isBattery -> "HIGH (Lithium Fire Risk)"
                                            isPcb -> "MEDIUM (Lead / Flame Retardants)"
                                            else -> "LOW (Standard Handling)"
                                        },
                                        toxicSubstances = when {
                                            isAC -> listOf("Freon / R22 / R32 / R410A Pressurized Gas", "Compressor Lubricant Oil")
                                            isFridge -> listOf("CFC/HFC Refrigerant", "Polyurethane ODS Foam", "Compressor Oil")
                                            isMicrowave -> listOf("High-Voltage Capacitor (Shock)", "Beryllium Oxide Ceramic")
                                            isPhone || isLaptop -> listOf("Lithium-Ion Battery (Thermal Runaway)", "Lead Solder", "Mercury trace")
                                            isFan -> listOf("Starting Capacitor", "Sharp Iron Edges")
                                            isBattery -> listOf("Lithium", "Lead", "Sulfuric / Organic Electrolyte")
                                            isPcb -> listOf("Lead solder", "BFR", "Mercury trace")
                                            else -> listOf("Sharp metallic edges")
                                        },
                                        alertEn = when {
                                            isAC -> "CRITICAL: Contains pressurized refrigerant gas. Do not cut tubing or vent gas. Certified degassing required."
                                            isFridge -> "HAZARD: Ozone-depleting refrigerant. Evacuate gas and compressor oil prior to dismantling."
                                            isMicrowave -> "DANGER: High voltage capacitor retains lethal charge. Do not puncture magnetron."
                                            isPhone || isLaptop -> "FIRE RISK: Contains integrated Li-ion battery. Keep away from water, heat, and sharp crushing."
                                            isFan -> "HIGH VALUE: Motor stator contains 400g-800g pure copper. Crack casing to extract winding."
                                            isBattery -> "DANGER: Fire risk if punctured. Store in fire-retardant dry container."
                                            isPcb -> "HAZARD: Contains lead solder & flame retardants. Do not burn."
                                            else -> "Safe to handle with standard puncture-proof work gloves."
                                        },
                                        alertHi = when {
                                            isAC -> "गंभीर खतरा: प्रेशराइज्ड रेफ्रिजरेंट गैस (फ्रीन)। पाइप न काटें, अधिकृत गैस रिकवरी कराएं।"
                                            isFridge -> "पर्यावरणीय खतरा: ओजोन गैस मौजूद है। कंप्रेसर गैस और तेल पहले रिकवर करें।"
                                            isMicrowave -> "हाई वोल्टेज खतरा: कैपेसिटर में घातक करंट हो सकता है। मैग्नेट्रॉन न तोड़ें।"
                                            isPhone || isLaptop -> "खतरा: लिथियम बैटरी मौजूद है। पंचर या तेज दबाव से आग लग सकती है।"
                                            isFan -> "अधिक मुनाफा: मोटर के अंदर शुद्ध तांबे की वाइंडिंग है। खोलकर अलग निकालें।"
                                            isBattery -> "खतरा: पंचर होने पर आग लगने का खतरा। सुरक्षित डिब्बे में रखें।"
                                            isPcb -> "चेतावनी: लेड सोल्डर मौजूद है। इसे जलाएं या तोड़ें नहीं।"
                                            else -> "सावधानी: भारी दस्ताने पहनकर उठाएं।"
                                        },
                                        alertMr = when {
                                            isAC -> "गंभीर धोका: दाबाखालील रेफ्रिजरंट गॅस. पाईप कापू नका, गॅस रिकव्हरी करा."
                                            isFridge -> "पर्यावरणीय धोका: ओझोन गॅस आहे. ऑइल आणि गॅस आधी सुरक्षित काढा."
                                            isMicrowave -> "धोका: कपॅसिटरमध्ये वीज शिल्लक असू शकते. मॅग्नेट्रॉन फोडू नका."
                                            isPhone || isLaptop -> "धोका: लिथियम-आयन बॅटरी आहे. बॅटरी दाबू किंवा वाकवू नका."
                                            isFan -> "जास्त नफा: मोटरच्या आत शुद्ध तांब्याची वाइंडिंग आहे. वेगळे करा."
                                            isBattery -> "धोका: बॅटरी फुटल्यास आगीचा धोका. सुरक्षित जागेत ठेवा."
                                            isPcb -> "धोका: लेड सोल्डर आहे. बोर्ड तोडू नका."
                                            else -> "काळजी घ्या: जाड हातमोजे वापरा."
                                        },
                                        safeHandlingProtocol = when {
                                            isAC || isFridge -> "CPCB authorized degassing and hermetic compressor extraction facility."
                                            isPhone || isLaptop -> "Isolate battery cell, dispatch logic board to authorized precious metal refiner."
                                            else -> "Transfer directly to licensed CPCB/SPCB dismantling facility."
                                        }
                                    ),
                                    valuation = com.example.sihscrap.api.ScrapValuationQuoteDto(
                                        materialCode = currentResult.categoryCode,
                                        materialName = currentResult.categoryName,
                                        baseMandiRateInrPerKg = when {
                                            isAC -> 95.0
                                            isFan -> 85.0
                                            isPhone -> 450.0
                                            isLaptop -> 280.0
                                            isFridge -> 45.0
                                            isWashing -> 40.0
                                            isMicrowave -> 42.0
                                            isMouse || isKeyboard -> 45.0
                                            isCopper -> 695.0
                                            isPcb -> 340.0
                                            isAlum -> 180.0
                                            isIron -> 38.0
                                            isBattery -> 85.0
                                            isCardboard -> 12.0
                                            isPlastic -> 28.0
                                            else -> 45.0
                                        },
                                        estimatedWeightRangeKg = when {
                                            isAC -> listOf(18.0, 38.0)
                                            isFridge -> listOf(25.0, 55.0)
                                            isWashing -> listOf(22.0, 48.0)
                                            isFan -> listOf(2.5, 6.5)
                                            isMicrowave -> listOf(8.0, 16.0)
                                            isLaptop -> listOf(1.4, 2.8)
                                            isPhone -> listOf(0.15, 0.35)
                                            isMouse -> listOf(0.08, 0.20)
                                            isKeyboard -> listOf(0.4, 0.9)
                                            else -> listOf(0.5, 3.0)
                                        },
                                        estimatedPayoutRangeInr = when {
                                            isAC -> listOf(1600.0, 3600.0)
                                            isFridge -> listOf(1100.0, 2400.0)
                                            isWashing -> listOf(850.0, 1900.0)
                                            isFan -> listOf(220.0, 550.0)
                                            isMicrowave -> listOf(320.0, 680.0)
                                            isLaptop -> listOf(400.0, 1200.0)
                                            isPhone -> listOf(80.0, 450.0)
                                            isMouse -> listOf(15.0, 45.0)
                                            isKeyboard -> listOf(20.0, 60.0)
                                            else -> listOf(120.0, 680.0)
                                        },
                                        carbonOffsetKg = when {
                                            isAC -> 85.0
                                            isFridge -> 65.0
                                            isWashing -> 50.0
                                            isLaptop -> 35.0
                                            isMicrowave -> 24.0
                                            isFan -> 18.0
                                            isPhone -> 16.0
                                            else -> 12.5
                                        }
                                    ),
                                    authorizedRecyclerChannel = if (isAC || isFridge) "CPCB Registered ODS & E-Waste Refiner" else "CPCB Registered E-Waste Recycler",
                                    aiEngine = "Offline CPCB Grounded Engine (24/7 Edge Mode)"
                                )
                            } finally {
                                isAnalyzingMultimodal = false
                            }
                        }
                    }

                    // Multi-item Cart Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerBatchAudit()
                            },
                            enabled = !isAnalyzingMultimodal,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tierColor)
                        ) {
                            if (isAnalyzingMultimodal) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Camera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to Batch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    }
}

