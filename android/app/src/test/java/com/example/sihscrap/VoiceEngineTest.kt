package com.example.sihscrap

import com.example.sihscrap.voice.VoiceNormalizer
import org.junit.Assert.*
import org.junit.Test

class VoiceEngineTest {

    @Test
    fun testMarathiAndHindiNumberNormalization() {
        assertEquals("0.5", VoiceNormalizer.normalizeNumbers("ardha"))
        assertEquals("0.5", VoiceNormalizer.normalizeNumbers("aadha"))
        assertEquals("1.5", VoiceNormalizer.normalizeNumbers("dedh"))
        assertEquals("2.5", VoiceNormalizer.normalizeNumbers("adhich"))
        assertEquals("2.5", VoiceNormalizer.normalizeNumbers("dhai"))
        assertEquals("2", VoiceNormalizer.normalizeNumbers("don"))
        assertEquals("5", VoiceNormalizer.normalizeNumbers("paanch"))
        assertEquals("10", VoiceNormalizer.normalizeNumbers("daha"))
        assertEquals("100", VoiceNormalizer.normalizeNumbers("shambhar"))
    }

    @Test
    fun testDevanagariNumeralsReplacement() {
        val devText = "१४.५ किलो तांबा"
        val normalized = VoiceNormalizer.normalizeNumbers(devText)
        assertTrue(normalized.contains("14.5"))
    }

    @Test
    fun testScrapSlangRecognition() {
        val slangs = VoiceNormalizer.recognizeSlang("aamhi don kilo tambha aani paach kilo lokhand viknar")
        assertTrue(slangs.contains("tambha"))
        assertTrue(slangs.contains("lokhand"))

        val hindiSlangs = VoiceNormalizer.recognizeSlang("peetal aur e-kachra ka bhav kya hai")
        assertTrue(hindiSlangs.contains("peetal"))
        assertTrue(hindiSlangs.contains("e-kachra"))
    }

    @Test
    fun testSlangToMaterialCodeMapping() {
        assertEquals("copper_bare_bright", VoiceNormalizer.mapSlangToMaterialCode(listOf("tambha")))
        assertEquals("brass_honey", VoiceNormalizer.mapSlangToMaterialCode(listOf("peetal")))
        assertEquals("heavy_steel_sariya", VoiceNormalizer.mapSlangToMaterialCode(listOf("lokhand")))
        assertEquals("high_grade_server_pcb", VoiceNormalizer.mapSlangToMaterialCode(listOf("e-kachra")))
        assertEquals("lead_acid_battery", VoiceNormalizer.mapSlangToMaterialCode(listOf("battery")))
        assertEquals("cardboard_carton", VoiceNormalizer.mapSlangToMaterialCode(listOf("raddi")))
        assertEquals("pet_plastic", VoiceNormalizer.mapSlangToMaterialCode(listOf("batli")))
    }

    @Test
    fun testWeightExtraction() {
        assertEquals(14.5, VoiceNormalizer.extractWeightKg("14.5 kilo tamba")!!, 0.01)
        assertEquals(2.5, VoiceNormalizer.extractWeightKg("2.5 kg loha")!!, 0.01)
        assertEquals(5.0, VoiceNormalizer.extractWeightKg("5 किलो")!!, 0.01)
    }

    @Test
    fun testIntentExtraction() {
        assertEquals("price_inquiry", VoiceNormalizer.extractIntent("tamba cha bhav kay aahe"))
        assertEquals("pickup_request", VoiceNormalizer.extractIntent("udya pickup pathva gharun"))
        assertEquals("valuation_submit", VoiceNormalizer.extractIntent("he kabaad jama kara vikri"))
    }

    @Test
    fun testFullVoiceCommandParsing() {
        val parsed = VoiceNormalizer.parseVoiceCommand("don kilo tamba bhav sanga")
        assertEquals("don kilo tamba bhav sanga", parsed.rawTranscript)
        assertTrue(parsed.detectedSlangs.contains("tamba"))
        assertEquals("copper_bare_bright", parsed.matchedMaterialCode)
        assertEquals(2.0, parsed.extractedWeightKg!!, 0.01)
        assertEquals("price_inquiry", parsed.intent)
    }
}
