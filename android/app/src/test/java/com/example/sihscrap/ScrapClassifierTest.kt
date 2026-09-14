package com.example.sihscrap

import com.example.sihscrap.ai.MaterialTier
import com.example.sihscrap.ai.ScrapClassifier
import org.junit.Assert.*
import org.junit.Test

class ScrapClassifierTest {

    @Test
    fun testMaterialTierClassification() {
        // Emerald Green: Clean Metals
        assertEquals(MaterialTier.EMERALD_GREEN, ScrapClassifier.getMaterialTier("copper_bare_bright"))
        assertEquals(MaterialTier.EMERALD_GREEN, ScrapClassifier.getMaterialTier("copper_armature"))
        assertEquals(MaterialTier.EMERALD_GREEN, ScrapClassifier.getMaterialTier("brass_honey"))
        assertEquals(MaterialTier.EMERALD_GREEN, ScrapClassifier.getMaterialTier("aluminium_extrusions"))

        // Amber: Mixed Scrap
        assertEquals(MaterialTier.AMBER, ScrapClassifier.getMaterialTier("heavy_steel_sariya"))
        assertEquals(MaterialTier.AMBER, ScrapClassifier.getMaterialTier("light_iron_patra"))
        assertEquals(MaterialTier.AMBER, ScrapClassifier.getMaterialTier("cast_iron"))
        assertEquals(MaterialTier.AMBER, ScrapClassifier.getMaterialTier("cardboard_carton"))
        assertEquals(MaterialTier.AMBER, ScrapClassifier.getMaterialTier("pet_plastic"))

        // Slate: Inert E-Waste
        assertEquals(MaterialTier.SLATE, ScrapClassifier.getMaterialTier("high_grade_server_pcb"))
        assertEquals(MaterialTier.SLATE, ScrapClassifier.getMaterialTier("mobile_phone_pcb"))

        // Crimson: Hazardous Battery/Lead
        assertEquals(MaterialTier.CRIMSON, ScrapClassifier.getMaterialTier("lead_acid_battery"))
        assertEquals(MaterialTier.CRIMSON, ScrapClassifier.getMaterialTier("li_ion_cells"))
    }

    @Test
    fun testPriceDeductionCalculation() {
        val basePrice = 1000.0

        // 0% rust -> no penalty
        val cleanPrice = ScrapClassifier.calculatePriceDeduction(basePrice, 0.0f)
        assertEquals(1000.0, cleanPrice, 0.01)

        // 50% rust -> 10% penalty
        val mediumRustPrice = ScrapClassifier.calculatePriceDeduction(basePrice, 50.0f)
        assertEquals(900.0, mediumRustPrice, 0.01)

        // 100% rust -> max 20% penalty
        val maxRustPrice = ScrapClassifier.calculatePriceDeduction(basePrice, 100.0f)
        assertEquals(800.0, maxRustPrice, 0.01)
    }
}
