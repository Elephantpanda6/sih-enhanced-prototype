package com.example.sihscrap.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

object AntiDoubleSpendEngine {
    private val secureRandom = SecureRandom()

    fun generateNonce(byteLength: Int = 16): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun generateReceiptHash(
        latitude: Double,
        longitude: Double,
        nonce: String,
        epochMillis: Long
    ): String {
        // Strict canonical format: LAT,LON:NONCE:EPOCH
        val canonicalCoordinates = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
        val rawData = "$canonicalCoordinates:$nonce:$epochMillis"
        return sha256(rawData)
    }

    fun verifyReceiptHash(
        receiptHash: String,
        latitude: Double,
        longitude: Double,
        nonce: String,
        epochMillis: Long
    ): Boolean {
        val expected = generateReceiptHash(latitude, longitude, nonce, epochMillis)
        return expected.equals(receiptHash, ignoreCase = true)
    }

    fun getHumanReadableReceiptId(receiptHash: String): String {
        val prefix = if (receiptHash.length >= 8) receiptHash.substring(0, 8).uppercase(Locale.US) else "SIH"
        return "RCPT-#$prefix"
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
