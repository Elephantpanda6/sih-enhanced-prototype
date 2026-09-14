package com.example.sihscrap

import org.junit.Assert.*
import org.junit.Test

class ValuationLogicTest {

    @Test
    fun testValuationWithEprSubsidyAndRustPenalty() {
        val weightKg = 14.5
        val spotRatePerKg = 850.0 // Server PCB
        val purity = 0.95
        val eprBonusPerKg = 7.0 // ₹7/kg extra CPCB subsidy
        val rustPercentage = 10.0f // 10% rust

        val grossValue = weightKg * spotRatePerKg * purity
        val rustDeductionPct = (rustPercentage / 100.0) * 0.20
        val rustPenaltyAmount = grossValue * rustDeductionPct
        val eprTotalBonus = weightKg * eprBonusPerKg
        val netPayout = grossValue - rustPenaltyAmount + eprTotalBonus

        // Gross = 14.5 * 850 * 0.95 = 11708.75
        assertEquals(11708.75, grossValue, 0.01)

        // Rust penalty = 11708.75 * 0.02 = 234.175
        assertEquals(234.175, rustPenaltyAmount, 0.01)

        // EPR Bonus = 14.5 * 7 = 101.5
        assertEquals(101.5, eprTotalBonus, 0.01)

        // Net = 11708.75 - 234.175 + 101.5 = 11576.075
        assertEquals(11576.075, netPayout, 0.01)
    }

    @Test
    fun testCo2SavingsCalculation() {
        val weightKg = 10.0
        val aluminiumCo2Factor = 9.0 // 9 kg CO2 saved per kg of recycled aluminium
        val totalCo2Saved = weightKg * aluminiumCo2Factor
        assertEquals(90.0, totalCo2Saved, 0.01)
    }
}
