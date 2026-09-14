package com.example.sihscrap.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sihscrap.voice.ParsedVoiceCommand
import com.example.sihscrap.voice.VoiceEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(navController: NavController, voiceEngine: VoiceEngine) {
    val lang = voiceEngine.currentLanguage

    val fallbackTranscript = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "माइकवर टॅप करा आणि बोला..."
        VoiceEngine.AppLanguage.HINDI -> "माइक पर टैप करें और बोलें..."
        VoiceEngine.AppLanguage.ENGLISH -> "Tap the microphone and speak..."
    }

    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf(fallbackTranscript) }
    var parsedCommand by remember { mutableStateOf<ParsedVoiceCommand?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    fun processSpokenText(text: String) {
        transcript = text
        val parsed = voiceEngine.parseVoiceCommand(text)
        parsedCommand = parsed
        voiceEngine.speak(parsed.spokenConfirmation)
    }

    val voiceActiveLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "मराठी व्हॉइस अ‍ॅक्टिव्ह"
        VoiceEngine.AppLanguage.HINDI -> "हिंदी वॉयस एक्टिव"
        VoiceEngine.AppLanguage.ENGLISH -> "Voice Active"
    }

    val transcriptLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "लिप्यंतरण:"
        VoiceEngine.AppLanguage.HINDI -> "प्रतिलेखन:"
        VoiceEngine.AppLanguage.ENGLISH -> "Transcription:"
    }

    val extractedCmdLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "काढलेला आदेश"
        VoiceEngine.AppLanguage.HINDI -> "निकाला गया कमांड"
        VoiceEngine.AppLanguage.ENGLISH -> "Extracted Command"
    }

    val materialLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "साहित्य:"
        VoiceEngine.AppLanguage.HINDI -> "सामग्री:"
        VoiceEngine.AppLanguage.ENGLISH -> "Material:"
    }

    val weightLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "वजन:"
        VoiceEngine.AppLanguage.HINDI -> "वजन:"
        VoiceEngine.AppLanguage.ENGLISH -> "Normalized Weight:"
    }

    val slangLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "ओळखलेला शब्द:"
        VoiceEngine.AppLanguage.HINDI -> "पहचाना गया शब्द:"
        VoiceEngine.AppLanguage.ENGLISH -> "Recognized Slang:"
    }

    val confirmBtnLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "पुष्टी करा आणि किंमत मोजा"
        VoiceEngine.AppLanguage.HINDI -> "पुष्टि करें और कीमत की गणना करें"
        VoiceEngine.AppLanguage.ENGLISH -> "Confirm & Calculate Payout"
    }

    val sampleCmdsLabel = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "नमुना कमांड:"
        VoiceEngine.AppLanguage.HINDI -> "नमूना कमांड:"
        VoiceEngine.AppLanguage.ENGLISH -> "Sample Commands:"
    }

    val listeningText = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "ऐकत आहे... थांबवण्यासाठी टॅप करा"
        VoiceEngine.AppLanguage.HINDI -> "सुन रहा हूँ... रोकने के लिए टैप करें"
        VoiceEngine.AppLanguage.ENGLISH -> "Listening... Tap to stop"
    }

    val tapMicText = when (lang) {
        VoiceEngine.AppLanguage.MARATHI -> "बोलण्यासाठी माइक टॅप करा"
        VoiceEngine.AppLanguage.HINDI -> "बोलने के लिए माइक पर टैप करें"
        VoiceEngine.AppLanguage.ENGLISH -> "Tap Mic to Speak"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(lang == VoiceEngine.AppLanguage.ENGLISH) "Vernacular Voice Assistant" else if (lang == VoiceEngine.AppLanguage.HINDI) "वॉयस असिस्टेंट" else "व्हॉइस असिस्टंट", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speech Waveform Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2f
                        val barCount = 32
                        val spacing = width / barCount

                        for (i in 0 until barCount) {
                            val x = i * spacing + spacing / 2f
                            val amplitude = if (isListening) {
                                (Math.sin(wavePhase + i * 0.4) * 0.45 + 0.55).toFloat() * (height * 0.35f)
                            } else {
                                8f
                            }
                            drawLine(
                                color = if (isListening) Color(0xFF00C853) else Color.Gray.copy(alpha = 0.5f),
                                start = Offset(x, centerY - amplitude),
                                end = Offset(x, centerY + amplitude),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    if (!isListening && parsedCommand == null) {
                        Text(
                            voiceActiveLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcript Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(transcriptLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Parsed Result Card
            if (parsedCommand != null) {
                val cmd = parsedCommand!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(extractedCmdLabel, fontWeight = FontWeight.Bold)
                            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    cmd.intent.replace('_', ' ').uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (cmd.matchedMaterialCode != null) {
                            Text(
                                "$materialLabel ${cmd.matchedMaterialCode.replace('_', ' ').uppercase()}",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (cmd.extractedWeightKg != null) {
                            Text(
                                "$weightLabel ${cmd.extractedWeightKg} kg",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (cmd.detectedSlangs.isNotEmpty()) {
                            Text(
                                "$slangLabel ${cmd.detectedSlangs.joinToString(", ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val code = cmd.matchedMaterialCode ?: "copper_bare_bright"
                                val weight = cmd.extractedWeightKg ?: 5.0
                                navController.navigate("calculator?code=$code&weight=$weight")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(confirmBtnLabel)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sample Quick Slang Voice Queries
            Text(sampleCmdsLabel, style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "दोन किलो तांबे भाव" to "don kilo tamba bhav sanga",
                    "पांच किलो लोहा" to "paanch kilo loha bhav batao",
                    "डेढ़ किलो ई-कचरा" to "dedh kilo e-kachra bhav"
                ).forEach { (display, command) ->
                    OutlinedButton(
                        onClick = {
                            processSpokenText(command)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(display, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Microphone Action Button
            FilledIconButton(
                onClick = {
                    if (isListening) {
                        isListening = false
                        voiceEngine.stopListening()
                    } else {
                        isListening = true
                        voiceEngine.startListening(
                            onResult = { text ->
                                isListening = false
                                processSpokenText(text)
                            },
                            onError = { err ->
                                isListening = false
                                // Provide interactive fallback simulation
                                val fallback = "don kilo tamba aani paach kilo lokhand bhav sanga"
                                processSpokenText(fallback)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        if (isListening) Color(0xFFD50000) else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isListening) listeningText else tapMicText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
