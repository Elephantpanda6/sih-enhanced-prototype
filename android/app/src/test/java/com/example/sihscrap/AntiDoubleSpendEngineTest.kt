package com.example.sihscrap

import com.example.sihscrap.data.security.AntiDoubleSpendEngine
import org.junit.Assert.*
import org.junit.Test

class AntiDoubleSpendEngineTest {

    @Test
    fun testReceiptHashGenerationAndVerification() {
        val lat = 18.520430
        val lon = 73.856740
        val nonce = AntiDoubleSpendEngine.generateNonce()
        val epoch = 1725619200000L

        val receiptHash = AntiDoubleSpendEngine.generateReceiptHash(lat, lon, nonce, epoch)

        assertNotNull(receiptHash)
        assertEquals(64, receiptHash.length) // SHA-256 output is 64 hex characters
        assertTrue(AntiDoubleSpendEngine.verifyReceiptHash(receiptHash, lat, lon, nonce, epoch))

        // Any tampering in coordinates or timestamp fails verification
        assertFalse(AntiDoubleSpendEngine.verifyReceiptHash(receiptHash, lat + 0.001, lon, nonce, epoch))
        assertFalse(AntiDoubleSpendEngine.verifyReceiptHash(receiptHash, lat, lon, nonce, epoch + 1))
    }

    @Test
    fun testNonceUniqueness() {
        val set = mutableSetOf<String>()
        for (i in 0 until 100) {
            val nonce = AntiDoubleSpendEngine.generateNonce()
            assertEquals(32, nonce.length) // 16 bytes = 32 hex chars
            assertFalse(set.contains(nonce))
            set.add(nonce)
        }
    }

    @Test
    fun testHumanReadableReceiptId() {
        val hash = "a3f9b2c1d4e5f607182930415263748596a7b8c9d0e1f2"
        val humanId = AntiDoubleSpendEngine.getHumanReadableReceiptId(hash)
        assertEquals("RCPT-#A3F9B2C1", humanId)
    }
}
