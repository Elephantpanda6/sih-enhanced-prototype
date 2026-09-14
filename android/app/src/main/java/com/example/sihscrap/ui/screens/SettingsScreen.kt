package com.example.sihscrap.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sihscrap.ai.OnDeviceVlmEngine
import com.example.sihscrap.voice.VoiceEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    voiceEngine: VoiceEngine? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vlmEngine = remember { OnDeviceVlmEngine(context) }

    var selectedTier by remember { mutableStateOf(vlmEngine.selectedTier) }
    var memoryStats by remember { mutableStateOf(vlmEngine.getMemoryStats()) }
    var isAllocating by remember { mutableStateOf(false) }
    var discoveredFile by remember { mutableStateOf(vlmEngine.discoveredModelFile) }
    var statusText by remember { mutableStateOf(vlmEngine.statusMessage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI & Hardware Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Snapdragon 8 Elite • 24 GB LPDDR5X",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: VISION MODEL TIER SELECTION
            Text(
                text = "🧠 Neural Vision Model Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose the AI reasoning engine used during camera scans for scrap valuation, rust estimation, and CPCB hazardous compliance auditing.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OnDeviceVlmEngine.RamTier.values().forEach { tier ->
                    val isSelected = selectedTier == tier
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTier = tier
                                scope.launch {
                                    isAllocating = true
                                    vlmEngine.warmUpModelInRamAsync(tier)
                                    memoryStats = vlmEngine.getMemoryStats()
                                    discoveredFile = vlmEngine.discoveredModelFile
                                    statusText = vlmEngine.statusMessage
                                    isAllocating = false
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedTier = tier
                                    scope.launch {
                                        isAllocating = true
                                        vlmEngine.warmUpModelInRamAsync(tier)
                                        memoryStats = vlmEngine.getMemoryStats()
                                        discoveredFile = vlmEngine.discoveredModelFile
                                        statusText = vlmEngine.statusMessage
                                        isAllocating = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tier.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (tier == OnDeviceVlmEngine.RamTier.TIER_7B_FP16) {
                                        Surface(
                                            color = Color(0xFFFF6D00),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "BEST QUALITY",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (tier == OnDeviceVlmEngine.RamTier.TIER_DUAL_ONNX) {
                                        Surface(
                                            color = Color(0xFF00C853),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ZERO DOWNLOAD",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = tier.approxParams,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tier.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Allocation Loading Indicator
            AnimatedVisibility(visible = isAllocating) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Memory mapping ${selectedTier.displayName} in LPDDR5X RAM...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // SECTION 2: GGUF WEIGHTS FILE MANAGEMENT
            Text(
                text = "📦 On-Device Weights & Downloads",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (discoveredFile != null) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (discoveredFile != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (discoveredFile != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Weights Ready for Native Execution",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Text("• Filename: ${discoveredFile?.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        vlmEngine.ggufInfo?.let { info ->
                            Text("• Architecture: ${info.architecture} | Model: ${info.modelName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Tensors: ${info.tensorCount} | Size: ${String.format("%.2f", info.fileSizeGb)} GB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Model Storage Directory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text("Path: /sdcard/Download/ or /sdcard/models/", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Expected file: Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf or Qwen2-VL-2B-Instruct-Q4_K_M.gguf", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ Direct 1-Tap Downloads (via Android DownloadManager):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                vlmEngine.downloadModelWeights()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 7B (4.68 GB)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                vlmEngine.downloadLightweightVisionModel()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                        ) {
                            Text("⚡ 2B (986 MB)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/ggml-org/Qwen2.5-VL-7B-Instruct-GGUF"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌐 HuggingFace", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                discoveredFile = vlmEngine.scanForModelFiles()
                                memoryStats = vlmEngine.getMemoryStats()
                                statusText = if (discoveredFile != null) "Found ${discoveredFile?.name}" else "No weights found in storage"
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Rescan", fontSize = 11.sp)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // SECTION 3: SNAPDRAGON 8 ELITE & TELEMETRY
            Text(
                text = "⚡ Snapdragon 8 Elite & Unified RAM Telemetry",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("SoC: Qualcomm Snapdragon 8 Elite (SM8750)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Text("• 2x Oryon Prime Cores @ 4.32 GHz", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("• 6x Oryon Performance Cores @ 3.53 GHz", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("• 24 GB LPDDR5X RAM (5300 MHz) | Large Heap", fontSize = 11.sp, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Committed VLM RAM: ${String.format("%.2f", memoryStats.allocatedVlmBufferGb)} GB",
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Process PSS: ${memoryStats.appProcessPssMb} MB | Native Heap: ${memoryStats.nativeHeapAllocatedMb} MB",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        "Device Free RAM: ${String.format("%.1f", memoryStats.deviceAvailableRamGb)} GB / ${String.format("%.1f", memoryStats.deviceTotalRamGb)} GB",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        "Status: $statusText",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isAllocating = true
                                vlmEngine.warmUpModelInRamAsync(selectedTier)
                                memoryStats = vlmEngine.getMemoryStats()
                                statusText = vlmEngine.statusMessage
                                isAllocating = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("🚀 Warm Up & Verify Model in RAM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
