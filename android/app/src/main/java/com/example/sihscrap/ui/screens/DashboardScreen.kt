package com.example.sihscrap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sihscrap.voice.VoiceEngine

data class VisualScrapCard(
    val code: String,
    val nameEn: String,
    val nameHi: String,
    val nameMr: String,
    val spotRate: Double,
    val eprBonus: Double,
    val tierColor: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    voiceEngine: VoiceEngine
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(voiceEngine.currentLanguage) }

    val cards = remember {
        listOf(
            VisualScrapCard("copper_bare_bright", "Copper Millberry", "तांबा (मिलबेरी)", "तांबे (मिलबेरी)", 695.0, 5.0, Color(0xFF00C853), Icons.Default.ElectricBolt),
            VisualScrapCard("brass_honey", "Yellow Brass", "पीतल (ब्रास)", "पितळ (ब्रास)", 460.0, 4.0, Color(0xFF00C853), Icons.Default.Star),
            VisualScrapCard("aluminium_extrusions", "Aluminium 6063", "एल्युमिनियम", "अ‍ॅल्युमिनियम", 195.0, 4.5, Color(0xFF00C853), Icons.Default.Shield),
            VisualScrapCard("heavy_steel_sariya", "Steel & Iron", "लोहा (सरिया)", "लोखंड (सळई)", 42.0, 3.0, Color(0xFFFFB300), Icons.Default.Hardware),
            VisualScrapCard("high_grade_server_pcb", "E-Waste PCBs", "सर्वर ई-कचरा", "सर्व्हर ई-कचरा", 850.0, 7.0, Color(0xFF607D8B), Icons.Default.Memory),
            VisualScrapCard("lead_acid_battery", "Batteries (HazMat)", "लेड एसिड बैटरी", "लेड अ‍ॅसिड बॅटरी", 95.0, 6.0, Color(0xFFD50000), Icons.Default.BatteryChargingFull)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (selectedLanguage) {
                                VoiceEngine.AppLanguage.MARATHI -> "स्मार्ट स्क्रॅप व्हॅल्युएशन"
                                VoiceEngine.AppLanguage.HINDI -> "स्मार्ट स्क्रैप मूल्यांकन"
                                VoiceEngine.AppLanguage.ENGLISH -> "SIH Smart Scrap Valuation"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Offline-Ready | 2G/EDGE Sync Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    // Audio guidance button for zero-literacy users
                    IconButton(
                        onClick = {
                            val prompt = when (selectedLanguage) {
                                VoiceEngine.AppLanguage.MARATHI -> "नमस्कार. स्क्रॅपचा दर पाहण्यासाठी खालील कार्डवर टॅप करा किंवा माइक दाबा."
                                VoiceEngine.AppLanguage.HINDI -> "नमस्ते. कबाड़ का भाव जानने के लिए नीचे किसी भी कार्ड पर टैप करें या माइक दबाएं."
                                VoiceEngine.AppLanguage.ENGLISH -> "Welcome. Tap any scrap card below or use the microphone to speak."
                            }
                            voiceEngine.speak(prompt)
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio Guidance", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Disguise Calculator Access
                    IconButton(
                        onClick = { navController.navigate("duress_calc") }
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Language Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceEngine.AppLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = {
                            selectedLanguage = lang
                            voiceEngine.currentLanguage = lang
                            voiceEngine.speak(lang.displayName)
                        },
                        label = { Text(lang.displayName, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Status Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Recycling, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CPCB/SPCB Subsidies: +₹3 to ₹7/kg", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { navController.navigate("history") }) {
                        Text("Ledger", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                when (selectedLanguage) {
                    VoiceEngine.AppLanguage.MARATHI -> "स्क्रॅप निवडा (तपशील पहा)"
                    VoiceEngine.AppLanguage.HINDI -> "स्क्रैप चुनें (विवरण देखें)"
                    VoiceEngine.AppLanguage.ENGLISH -> "Select Scrap Category"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )

            // High-Contrast Visual Flashcards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cards) { card ->
                    val displayName = when (selectedLanguage) {
                        VoiceEngine.AppLanguage.MARATHI -> card.nameMr
                        VoiceEngine.AppLanguage.HINDI -> card.nameHi
                        VoiceEngine.AppLanguage.ENGLISH -> card.nameEn
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable {
                                val msg = when (selectedLanguage) {
                                    VoiceEngine.AppLanguage.MARATHI -> "$displayName, ₹${card.spotRate.toInt()} प्रति किलो"
                                    VoiceEngine.AppLanguage.HINDI -> "$displayName, ₹${card.spotRate.toInt()} प्रति किलो"
                                    VoiceEngine.AppLanguage.ENGLISH -> "$displayName, ₹${card.spotRate.toInt()} per kg"
                                }
                                voiceEngine.speak(msg)
                                navController.navigate("calculator?code=${card.code}&rate=${card.spotRate}&epr=${card.eprBonus}&name=${card.nameEn}")
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(card.tierColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(card.icon, contentDescription = null, tint = card.tierColor, modifier = Modifier.size(22.dp))
                                }
                                Surface(
                                    color = card.tierColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "₹${card.spotRate.toInt()}/kg",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Column {
                                Text(displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                Text("+₹${card.eprBonus.toInt()}/kg EPR Subsidy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Zero-Literacy Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Voice Assistant Button (min 56dp height)
                Button(
                    onClick = {
                        val msg = when (selectedLanguage) {
                            VoiceEngine.AppLanguage.MARATHI -> "माइक सुरू झाला आहे. बोला."
                            VoiceEngine.AppLanguage.HINDI -> "माइक चालू है। बोलें।"
                            VoiceEngine.AppLanguage.ENGLISH -> "Microphone is on. Speak now."
                        }
                        voiceEngine.speak(msg)
                        navController.navigate("voice")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (selectedLanguage) {
                            VoiceEngine.AppLanguage.MARATHI -> "आवाज (बोला)"
                            VoiceEngine.AppLanguage.HINDI -> "आवाज़ (बोलें)"
                            VoiceEngine.AppLanguage.ENGLISH -> "Voice"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // AI Camera Scanner Button (min 56dp height)
                Button(
                    onClick = { navController.navigate("camera") },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (selectedLanguage) {
                            VoiceEngine.AppLanguage.MARATHI -> "कॅमेरा स्कॅन"
                            VoiceEngine.AppLanguage.HINDI -> "कैमरा स्कैन"
                            VoiceEngine.AppLanguage.ENGLISH -> "AI Scan"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // CPCB Locator Button (min 56dp height)
                FilledTonalButton(
                    onClick = { navController.navigate("locator") },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when (selectedLanguage) {
                            VoiceEngine.AppLanguage.MARATHI -> "केंद्रे"
                            VoiceEngine.AppLanguage.HINDI -> "केंद्र"
                            VoiceEngine.AppLanguage.ENGLISH -> "Recyclers"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
