package com.example.sihscrap

import com.example.sihscrap.hardware.BleWeightParser
import org.junit.Assert.*
import org.junit.Test

class BleScaleServiceTest {

    @Test
    fun testParseWeightDataKgStable() {
        // Flags: 0x00 (SI units / kg, stable)
        // Weight: 2900 * 0.005 = 14.50 kg
        // 2900 in hex = 0x0B54 -> low byte = 0x54 (84), high byte = 0x0B (11)
        val data = byteArrayOf(0x00, 0x54, 0x0B)
        val (weight, isStable) = BleWeightParser.parse(data)

        assertEquals(14.50, weight, 0.01)
        assertTrue(isStable)
    }

    @Test
    fun testParseWeightDataLbsInMotion() {
        // Flags: 0x03 (Imperial lbs, in-motion / unstable)
        // Weight: 2000 * 0.005 = 10.0 lbs -> 10 * 0.45359237 = 4.54 kg
        // 2000 in hex = 0x07D0 -> low = 0xD0 (-48), high = 0x07 (7)
        val data = byteArrayOf(0x03, 0xD0.toByte(), 0x07)
        val (weight, isStable) = BleWeightParser.parse(data)

        assertEquals(4.54, weight, 0.05)
        assertFalse(isStable)
    }
}
