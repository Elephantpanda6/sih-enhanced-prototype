package com.example.sihscrap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sihscrap.data.ScrapRepository
import com.example.sihscrap.data.local.MaterialCatalogEntity
import com.example.sihscrap.data.security.AntiDoubleSpendEngine
import com.example.sihscrap.hardware.BleScaleService
import com.example.sihscrap.hardware.HapticHoldToConfirmButton
import com.example.sihscrap.voice.VoiceEngine
import com.example.sihscrap.ui.SharedViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValuationScreen(
    navController: NavController,
    voiceEngine: VoiceEngine,
    sharedViewModel: SharedViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScrapRepository(context) }
    val bleScaleService = remember { BleScaleService(context) }

    var catalog by remember { mutableStateOf<List<MaterialCatalogEntity>>(emptyList()) }
    var isSubmitted by remember { mutableStateOf(false) }
    var generatedReceiptHash by remember { mutableStateOf<String?>(null) }
    var globalFinalPayout by remember { mutableStateOf(0.0) }

    val scaleReading by bleScaleService.readingFlow.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            bleScaleService.stopListening()
        }
    }

    LaunchedEffect(Unit) {
        val list = repository.getCatalog()
        catalog = list
        repository.refreshCatalogFromBackend()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Order (${sharedViewModel.cart.size} items)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        if (sharedViewModel.cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your batch is empty.", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }

        var totalOrderValue = 0.0
        var totalCo2Saved = 0.0

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Scale Reading section
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BLE Hardware Scale", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { bleScaleService.startListening() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Scale")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Scale Reading: ${scaleReading.weightKg} kg",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (scaleReading.isStable) Color(0xFF00C853) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(sharedViewModel.cart) { index, item ->
                    val material = catalog.firstOrNull { it.code == item.code }
                        ?: MaterialCatalogEntity("unknown", "Unknown", "अज्ञात", "अज्ञात", "misc", 0.0, 0.0, 1.0, 0.0, "GRAY")

                    val spotRate = material.spotRatePerKg
                    val eprBonusPerKg = material.eprSubsidyBonusPerKg
                    val purity = material.defaultPurity

                    val currentWeight = item.weight
                    val grossValue = currentWeight * spotRate * purity
                    val rustDeductionPct = (item.rust / 100.0) * 0.15 // Backend uses ~15% baseline penalty
                    val rustDeductionAmount = grossValue * rustDeductionPct
                    val eprTotalBonus = currentWeight * eprBonusPerKg
                    
                    val grossMaterialValue = Math.max(0.0, grossValue - rustDeductionAmount)
                    
                    val processingFeePerKg = when {
                        item.code.contains("e_waste") -> 6.0
                        item.code.contains("battery") -> 4.0
                        else -> 1.5
                    }
                    val processingFee = currentWeight * processingFeePerKg
                    val dealerMargin = grossMaterialValue * 0.10 // 10% dealer margin
                    
                    val finalPayout = Math.max(0.0, grossMaterialValue - processingFee - dealerMargin + eprTotalBonus)
                    val co2Saved = currentWeight * material.co2SavingPerKg

                    totalOrderValue += finalPayout
                    totalCo2Saved += co2Saved

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹${String.format(Locale.US, "%.2f", finalPayout)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { sharedViewModel.removeFromCart(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Weight: ", fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = item.weight.toString(),
                                    onValueChange = { 
                                        val w = it.toDoubleOrNull()
                                        if (w != null) {
                                            sharedViewModel.updateWeight(index, w)
                                        }
                                    },
                                    modifier = Modifier.width(80.dp).height(48.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (scaleReading.weightKg > 0.0) {
                                    FilledTonalButton(onClick = { sharedViewModel.updateWeight(index, scaleReading.weightKg) }, modifier = Modifier.height(48.dp)) {
                                        Text("Use Scale")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Rust: ${item.rust.toInt()}% | CO2 Saved: ${String.format(Locale.US, "%.1f", co2Saved)}kg", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Order Value:", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    "₹${String.format(Locale.US, "%.2f", totalOrderValue)}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Anti-Accidental 2-Second Haptic Hold Button
            HapticHoldToConfirmButton(
                text = "Hold 2s to Submit Batch",
                onConfirmed = {
                    scope.launch {
                        val epochMillis = System.currentTimeMillis()
                        val nonce = AntiDoubleSpendEngine.generateNonce()
                        // 18.5204, 73.8567 (Pune)
                        val batchReceiptHash = AntiDoubleSpendEngine.generateReceiptHash(18.5204, 73.8567, nonce, epochMillis)

                        for (item in sharedViewModel.cart) {
                            repository.createSubmission(
                                materialCode = item.code,
                                weightKg = item.weight,
                                rustPercentage = item.rust,
                                latitude = 18.5204,
                                longitude = 73.8567,
                                voiceTranscript = "Batch order calculation",
                                receiptHashOverride = batchReceiptHash
                            )
                        }
                        
                        generatedReceiptHash = batchReceiptHash
                        globalFinalPayout = totalOrderValue
                        isSubmitted = true
                        sharedViewModel.clearCart()
                        val msg = when (voiceEngine.currentLanguage) {
                            VoiceEngine.AppLanguage.MARATHI -> "तुमची ऑर्डर यशस्वीरित्या नोंदवली गेली आहे."
                            VoiceEngine.AppLanguage.HINDI -> "आपका ऑर्डर सफलतापूर्वक सबमिट हो गया है।"
                            VoiceEngine.AppLanguage.ENGLISH -> "Batch order submitted successfully."
                        }
                        voiceEngine.speak(msg)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Submission Confirmation Dialog
    if (isSubmitted && generatedReceiptHash != null) {
        val shortId = AntiDoubleSpendEngine.getHumanReadableReceiptId(generatedReceiptHash!!)
        AlertDialog(
            onDismissRequest = { isSubmitted = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(48.dp)) },
            title = { Text("Batch Confirmed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Receipt Hash: $shortId", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Payout: ₹${String.format(Locale.US, "%.2f", globalFinalPayout)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Queued in offline database. Will sync immediately upon connection.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitted = false
                        navController.navigate("history")
                    }
                ) {
                    Text("View in Ledger")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isSubmitted = false
                        navController.navigate("dashboard")
                    }
                ) {
                    Text("Dashboard")
                }
            }
        )
    }
}
