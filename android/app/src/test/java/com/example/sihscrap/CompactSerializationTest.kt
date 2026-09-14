package com.example.sihscrap

import com.example.sihscrap.data.local.TransactionEntity
import com.example.sihscrap.data.sync.CompactPayloadSerializer
import org.junit.Assert.*
import org.junit.Test

class CompactSerializationTest {

    @Test
    fun testPayloadSerializationUnder310BytesConstraint() {
        val tx = TransactionEntity().apply {
            this.receiptHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            this.materialCode = "high_grade_server_pcb"
            this.weightKg = 14.50
            this.finalPayout = 12325.00
            this.gpsLatitude = 18.52043
            this.gpsLongitude = 73.85674
            this.nonce = "f4b8c9d0e1f2a3b4c5d6e7f8091a2b3c"
            this.epochMillis = 1725619200000L
            this.voiceTranscript = "14.5 kilo pcb bhav"
        }

        val json = CompactPayloadSerializer.serialize(tx)
        val byteSize = CompactPayloadSerializer.getPayloadSizeBytes(json)

        println("Serialized JSON: $json (size: $byteSize bytes)")

        assertTrue("Compact payload size ($byteSize bytes) must be <= 310 bytes", byteSize <= 310)
        assertTrue(CompactPayloadSerializer.isWithinBandwidthConstraint(json))

        // Deserialization fidelity
        val parsed = CompactPayloadSerializer.deserialize(json)
        assertEquals(tx.receiptHash, parsed.receiptHash)
        assertEquals(tx.materialCode, parsed.materialCode)
        assertEquals(tx.weightKg, parsed.weightKg, 0.001)
        assertEquals(tx.finalPayout, parsed.finalPayout, 0.001)
        assertEquals(tx.gpsLatitude, parsed.latitude, 0.0001)
        assertEquals(tx.gpsLongitude, parsed.longitude, 0.0001)
        assertEquals(tx.nonce, parsed.nonce)
        assertEquals(tx.epochMillis, parsed.epochMillis)
    }
}
