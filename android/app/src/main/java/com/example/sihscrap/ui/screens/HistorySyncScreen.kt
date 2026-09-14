package com.example.sihscrap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.sihscrap.data.local.TransactionEntity
import com.example.sihscrap.data.security.AntiDoubleSpendEngine
import com.example.sihscrap.data.sync.SyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySyncScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScrapRepository(context) }

    var transactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshList() {
        scope.launch {
            isRefreshing = true
            transactions = repository.getAllTransactions()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    val pendingCount = transactions.count { it.status == TransactionEntity.STATUS_PENDING_SYNC }
    val syncedCount = transactions.count { it.status == TransactionEntity.STATUS_SYNCED }
    val failedCount = transactions.count { it.status == TransactionEntity.STATUS_FAILED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Ledger & Sync", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            SyncWorker.syncNow(context)
                            refreshList()
                        }
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Sync Telemetry Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$syncedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00C853)))
                        Text("Synced", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pendingCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFFB300)))
                        Text("Pending Sync", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$failedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD50000)))
                        Text("Failed", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No submissions yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        Text("Calculate and submit scrap to see it here.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { tx ->
                        val receiptId = AntiDoubleSpendEngine.getHumanReadableReceiptId(tx.receiptHash ?: "")
                        val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.epochMillis))

                        val statusColor = when (tx.status) {
                            TransactionEntity.STATUS_SYNCED -> Color(0xFF00C853)
                            TransactionEntity.STATUS_FAILED -> Color(0xFFD50000)
                            else -> Color(0xFFFFB300)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxForDetails = tx },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(receiptId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Surface(
                                        color = statusColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tx.status ?: "PENDING",
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.materialName ?: tx.materialCode ?: "Scrap", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text("${tx.weightKg} kg | $formattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "₹${String.format(Locale.US, "%.2f", tx.finalPayout)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }

                                if (tx.status != TransactionEntity.STATUS_SYNCED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.retrySync(tx.id)
                                                    refreshList()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry Sync", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cryptographic Receipt Details Dialog
    if (selectedTxForDetails != null) {
        val tx = selectedTxForDetails!!
        AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            title = { Text("Receipt Cryptography", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Material: ${tx.materialName}")
                    Text("Weight: ${tx.weightKg} kg")
                    Text("Spot Rate: ₹${tx.spotRatePerKg}/kg (+₹${tx.eprBonusPerKg} EPR)")
                    Text("Total Payout: ₹${String.format(Locale.US, "%.2f", tx.finalPayout)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GPS Coordinates: ${tx.gpsLatitude}, ${tx.gpsLongitude}", style = MaterialTheme.typography.labelSmall)
                    Text("Nonce: ${tx.nonce}", style = MaterialTheme.typography.labelSmall)
                    Text("Epoch: ${tx.epochMillis}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("SHA-256 Receipt Hash:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(tx.receiptHash ?: "N/A", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Anti-Double-Spend Verified ✓", color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { selectedTxForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}
