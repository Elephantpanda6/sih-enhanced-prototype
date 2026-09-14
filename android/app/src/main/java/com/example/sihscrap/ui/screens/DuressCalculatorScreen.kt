package com.example.sihscrap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sihscrap.data.ScrapRepository
import com.example.sihscrap.hardware.HapticHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuressCalculatorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScrapRepository(context) }

    var displayText by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var isDecoyModeActive by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (displayText == "0" || displayText == "Error") {
            displayText = d
        } else {
            displayText += d
        }
        expression += d
    }

    fun onOperator(op: String) {
        expression += " $op "
        displayText = "0"
    }

    fun onClear() {
        displayText = "0"
        expression = ""
    }

    fun onEquals() {
        val cleanExpr = expression.trim().replace(" ", "")

        // Check for Secret Duress Codes (911, 999, 112)
        if (cleanExpr == "911" || cleanExpr == "999" || cleanExpr == "112" || displayText == "911" || displayText == "112") {
            // 1. Silent SOS Vibrate Telemetry
            HapticHelper.vibrateSosDuress(context)

            // 2. Transmit Silent ERSS 112 SOS Alert
            scope.launch {
                repository.sendDuressAlert(18.5204, 73.8567, cleanExpr)
            }

            // 3. Switch to Zero-Balance Decoy Screen immediately
            isDecoyModeActive = true
            return
        }

        // Standard Operational Arithmetic Evaluation
        try {
            val parts = expression.trim().split(" ")
            if (parts.size >= 3) {
                val left = parts[0].toDouble()
                val op = parts[1]
                val right = parts[2].toDouble()
                val res = when (op) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> if (right != 0.0) left / right else Double.NaN
                    else -> right
                }
                displayText = if (res.isNaN()) "Error" else if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
                expression = displayText
            }
        } catch (e: Exception) {
            displayText = "Error"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDecoyModeActive) "Passbook Summary" else "Standard Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isDecoyModeActive) {
            // ZERO-BALANCE DECOY SCREEN
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "₹0.00",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Total Cash Balance / Cleared Dues",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ledger Settlement Status", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Cash in Transit: Nil (₹0)")
                        Text("• Daily Dispatches: All items transferred to CPCB facility")
                        Text("• No physical cash or scrap stored at this location.")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bank Auto-Sweep Reference: #UTR-CLEAR-09", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        isDecoyModeActive = false
                        onClear()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Calculator")
                }
            }
        } else {
            // FULLY OPERATIONAL DISGUISE CALCULATOR
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Calculator Display Screen
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = expression,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculator Button Keypad
                val buttons = listOf(
                    listOf("C", "(", ")", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "911", "=")
                )

                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { label ->
                                val isOp = label in listOf("/", "*", "-", "+", "=")
                                val isSpecial = label == "C" || label == "911"

                                Button(
                                    onClick = {
                                        when (label) {
                                            "C" -> onClear()
                                            "=" -> onEquals()
                                            "911" -> {
                                                displayText = "911"
                                                expression = "911"
                                            }
                                            in listOf("+", "-", "*", "/") -> onOperator(label)
                                            else -> onDigit(label)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f / (6 - buttons.indexOf(row))),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (label == "=") MaterialTheme.colorScheme.primary
                                        else if (isOp) MaterialTheme.colorScheme.secondaryContainer
                                        else if (isSpecial) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (label == "=") Color.White
                                        else if (isSpecial) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
