package com.example.sihscrap.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

data class ParsedVoiceCommand(
    val rawTranscript: String,
    val normalizedText: String,
    val detectedSlangs: List<String>,
    val matchedMaterialCode: String?,
    val extractedWeightKg: Double?,
    val intent: String,
    val spokenConfirmation: String
)

object VoiceNormalizer {
    private fun levenshtein(s1: String, s2: String): Int {
        val edits = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) edits[i][0] = i
        for (j in 0..s2.length) edits[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                edits[i][j] = minOf(
                    edits[i - 1][j] + 1,
                    edits[i][j - 1] + 1,
                    edits[i - 1][j - 1] + cost
                )
            }
        }
        return edits[s1.length][s2.length]
    }

    private fun isFuzzyMatch(word: String, target: String): Boolean {
        if (word == target) return true
        if (word.length <= 3 || target.length <= 3) return word == target
        val distance = levenshtein(word, target)
        val allowedTypos = if (target.length <= 5) 1 else 2
        return distance <= allowedTypos
    }

    fun normalizeNumbers(text: String): String {
        var result = text.lowercase()

        val devanagariDigits = mapOf(
            '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
            '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9'
        )
        for ((dev, eng) in devanagariDigits) {
            result = result.replace(dev, eng)
        }

        val fractionMap = mapOf(
            "paav" to "0.25", "paon" to "0.25", "पाव" to "0.25",
            "ardha" to "0.5", "aadha" to "0.5", "अर्धा" to "0.5", "आधा" to "0.5",
            "paun" to "0.75", "paune" to "0.75", "पाऊण" to "0.75", "पौने" to "0.75",
            "sava" to "1.25", "सवा" to "1.25",
            "dedh" to "1.5", "दीड" to "1.5", "डेढ़" to "1.5",
            "adhich" to "2.5", "dhai" to "2.5", "अडीच" to "2.5", "ढाई" to "2.5",
            "aute" to "3.5", "औटे" to "3.5"
        )
        for ((word, num) in fractionMap) {
            result = result.replace(Regex("(?i)\\b$word\\b"), num)
        }

        val wholeNumberMap = mapOf(
            "ek" to "1", "एक" to "1", "one" to "1",
            "don" to "2", "do" to "2", "दोन" to "2", "दो" to "2", "two" to "2",
            "teen" to "3", "तीन" to "3", "three" to "3",
            "char" to "4", "chaar" to "4", "चार" to "4", "four" to "4",
            "paach" to "5", "paanch" to "5", "पाच" to "5", "पांच" to "5", "five" to "5",
            "saha" to "6", "chhah" to "6", "सहा" to "6", "छह" to "6", "six" to "6",
            "saat" to "7", "सात" to "7", "seven" to "7",
            "aath" to "8", "आठ" to "8", "eight" to "8",
            "nau" to "9", "nav" to "9", "नऊ" to "9", "नौ" to "9", "nine" to "9",
            "daha" to "10", "das" to "10", "दहा" to "10", "दस" to "10", "ten" to "10",
            "pandhra" to "15", "pandrah" to "15", "पंधरा" to "15", "पंद्रह" to "15", "fifteen" to "15",
            "vis" to "20", "bees" to "20", "वीस" to "20", "बीस" to "20", "twenty" to "20",
            "panchvis" to "25", "pachees" to "25", "पंचवीस" to "25", "पच्चीस" to "25", "twenty five" to "25",
            "tis" to "30", "tees" to "30", "तीस" to "30", "thirty" to "30",
            "chalis" to "40", "chaalis" to "40", "चाळीस" to "40", "चालीस" to "40", "forty" to "40",
            "pannas" to "50", "pachas" to "50", "पन्नास" to "50", "पचास" to "50", "fifty" to "50",
            "shambhar" to "100", "sau" to "100", "शंभर" to "100", "सौ" to "100", "hundred" to "100"
        )
        for ((word, num) in wholeNumberMap) {
            result = result.replace(Regex("(?i)\\b$word\\b"), num)
        }

        return result
    }

    private val scrapMapping = mapOf(
        "copper_bare_bright" to listOf("tambha", "tamba", "taamba", "copper", "तांबा", "तांबे", "copar", "coper"),
        "brass_honey" to listOf("peetal", "pital", "brass", "पीतल", "पितळ", "bras"),
        "heavy_steel_sariya" to listOf("lokhand", "loha", "sariya", "लोहा", "लोखंड", "सरिया", "iron", "steel", "stil"),
        "light_iron_patra" to listOf("patra", "पत्रा", "tin", "sheet"),
        "aluminium_extrusions" to listOf("aluminium", "एल्युमिनियम", "अ‍ॅल्युमिनियम", "aluminam", "alumunium", "aluminum"),
        "high_grade_server_pcb" to listOf("e-kachra", "pcb", "ई-कचरा", "ewaste", "e-waste", "circuit", "board", "motherboard"),
        "lead_acid_battery" to listOf("battery", "बैटरी", "बॅटरी", "bateri", "inverter"),
        "cardboard_carton" to listOf("raddi", "kabaad", "रद्दी", "कबाड़", "paper", "cardboard", "pudha", "puttha", "carton"),
        "pet_plastic" to listOf("batli", "bottle", "plastic", "प्लास्टिक", "प्लॅस्टिक", "botle", "botal")
    )

    fun recognizeSlang(text: String): List<String> {
        val found = mutableListOf<String>()
        val words = text.lowercase().split(Regex("[\\s,]+"))
        for (w in words) {
            val clean = w.replace(Regex("[^a-zA-Z\u0900-\u097F\\-]"), "").trim()
            if (clean.isEmpty()) continue
            
            var matchedTarget: String? = null
            // Check exact matches first
            for ((_, targets) in scrapMapping) {
                for (target in targets) {
                    if (clean == target) {
                        matchedTarget = target
                        break
                    }
                }
                if (matchedTarget != null) break
            }
            // If no exact match, try fuzzy matching
            if (matchedTarget == null) {
                for ((_, targets) in scrapMapping) {
                    for (target in targets) {
                        if (isFuzzyMatch(clean, target)) {
                            matchedTarget = target
                            break
                        }
                    }
                    if (matchedTarget != null) break
                }
            }
            if (matchedTarget != null && !found.contains(matchedTarget)) {
                found.add(matchedTarget)
            }
        }
        return found
    }


    fun mapSlangToMaterialCode(slangs: List<String>): String? {
        for (s in slangs) {
            for ((code, targets) in scrapMapping) {
                if (targets.contains(s)) return code
            }
        }
        return null
    }

    fun extractWeightKg(normalizedText: String): Double? {
        val pattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kilo|kg|किलो|केजी|kilogram)")
        val match = pattern.find(normalizedText)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }
        val words = normalizedText.split(" ")
        for (w in words) {
            val d = w.toDoubleOrNull()
            if (d != null && d > 0.0 && d < 10000.0) {
                return d
            }
        }
        return null
    }

    fun extractIntent(text: String): String {
        val t = text.lowercase()
        val intents = mapOf(
            "pickup_request" to listOf("pickup", "pathva", "bhejo", "booking", "book", "aao", "ya", "pathav"),
            "valuation_submit" to listOf("submit", "jama", "confirm", "vikri", "bechna", "bechu", "sell", "done"),
            "weight_query" to listOf("wajan", "vajan", "weight", "tolo", "moza", "wazan")
        )
        
        val words = t.split(Regex("[\\s,]+")).map { it.replace(Regex("[^a-zA-Z\u0900-\u097F]"), "") }
        for (w in words) {
            for ((intent, triggers) in intents) {
                for (trigger in triggers) {
                    if (isFuzzyMatch(w, trigger)) return intent
                }
            }
        }
        return "price_inquiry"
    }

    fun parseVoiceCommand(rawTranscript: String, lang: VoiceEngine.AppLanguage = VoiceEngine.AppLanguage.HINDI): ParsedVoiceCommand {
        val normalized = normalizeNumbers(rawTranscript)
        val slangs = recognizeSlang(normalized)
        val materialCode = mapSlangToMaterialCode(slangs)
        val weight = extractWeightKg(normalized)
        val intent = extractIntent(normalized)

        val readableMaterial = materialCode?.replace('_', ' ')?.uppercase() ?: "Scrap"
        val confirmation = when (lang) {
            VoiceEngine.AppLanguage.MARATHI -> if (weight != null) "$weight किलो $readableMaterial समजले आहे." else "$readableMaterial बद्दल विचारपूस."
            VoiceEngine.AppLanguage.HINDI -> if (weight != null) "$weight किलो $readableMaterial समझ लिया गया है।" else "$readableMaterial के बारे में पूछताछ।"
            VoiceEngine.AppLanguage.ENGLISH -> if (weight != null) "$weight kilos $readableMaterial parsed." else "$readableMaterial inquiry parsed."
        }

        return ParsedVoiceCommand(
            rawTranscript = rawTranscript,
            normalizedText = normalized,
            detectedSlangs = slangs,
            matchedMaterialCode = materialCode,
            extractedWeightKg = weight,
            intent = intent,
            spokenConfirmation = confirmation
        )
    }
}


class VoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "VoiceEngine"
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    enum class AppLanguage(val code: String, val displayName: String, val locale: Locale) {
        HINDI("hi-IN", "हिन्दी", Locale("hi", "IN")),
        MARATHI("mr-IN", "मराठी", Locale("mr", "IN")),
        ENGLISH("en-IN", "English", Locale("en", "IN"))
    }

    var currentLanguage: AppLanguage = AppLanguage.HINDI
        set(value) {
            field = value
            updateTtsLanguage()
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            updateTtsLanguage()
            tts.setSpeechRate(1.10f) // Speed up the voice of the system
            Log.d(TAG, "TTS initialized successfully.")
        } else {
            Log.w(TAG, "TTS initialization failed with status: $status")
        }
    }

    private fun updateTtsLanguage() {
        if (!isTtsReady) return
        val res = tts.setLanguage(currentLanguage.locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.US
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SIH_TTS_${System.currentTimeMillis()}")
        }
    }

    fun speakValuation(weightKg: Double, materialName: String, payoutInr: Double, lang: AppLanguage = currentLanguage) {
        val roundedWeight = Math.round(weightKg * 10.0) / 10.0
        val roundedPayout = Math.round(payoutInr).toInt()

        val text = when (lang) {
            AppLanguage.MARATHI -> "$roundedWeight किलो $materialName, एकूण $roundedPayout रुपये."
            AppLanguage.HINDI -> "$roundedWeight किलो $materialName, कुल $roundedPayout रुपये."
            AppLanguage.ENGLISH -> "$roundedWeight kilograms $materialName, total payout $roundedPayout rupees."
        }
        speak(text)
    }

    fun normalizeNumbers(text: String): String = VoiceNormalizer.normalizeNumbers(text)
    fun recognizeSlang(text: String): List<String> = VoiceNormalizer.recognizeSlang(text)
    fun mapSlangToMaterialCode(slangs: List<String>): String? = VoiceNormalizer.mapSlangToMaterialCode(slangs)
    fun extractWeightKg(normalizedText: String): Double? = VoiceNormalizer.extractWeightKg(normalizedText)
    fun extractIntent(text: String): String = VoiceNormalizer.extractIntent(text)
    fun parseVoiceCommand(rawTranscript: String): ParsedVoiceCommand = VoiceNormalizer.parseVoiceCommand(rawTranscript, currentLanguage)


    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    onError("Speech error code: $error")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    } else {
                        onError("No speech recognized")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.code)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            val promptMsg = when (currentLanguage) {
                AppLanguage.MARATHI -> "भंगाराचे नाव आणि वजन सांगा..."
                AppLanguage.HINDI -> "कबाड़ का नाम और वजन बताएं..."
                AppLanguage.ENGLISH -> "Speak scrap items (e.g., '2 kilos copper')..."
            }
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptMsg)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to start speech recognizer")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        stopListening()
    }
}
