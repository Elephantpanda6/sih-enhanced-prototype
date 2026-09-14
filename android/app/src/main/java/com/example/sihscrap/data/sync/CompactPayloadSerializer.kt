package com.example.sihscrap.data.sync

import com.example.sihscrap.data.local.TransactionEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Locale

data class CompactSubmissionPayload(
    @SerializedName("h") val receiptHash: String,
    @SerializedName("m") val materialCode: String,
    @SerializedName("w") val weightKg: Double,
    @SerializedName("p") val finalPayout: Double,
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("n") val nonce: String,
    @SerializedName("t") val epochMillis: Long,
    @SerializedName("v") val voiceTranscript: String? = null
)

object CompactPayloadSerializer {
    private val gson = Gson()
    const val MAX_ALLOWED_PAYLOAD_BYTES = 310

    fun serialize(tx: TransactionEntity): String {
        val truncatedTranscript = tx.voiceTranscript?.let {
            if (it.length > 30) it.substring(0, 30) else it
        }
        val payload = CompactSubmissionPayload(
            receiptHash = tx.receiptHash ?: "",
            materialCode = tx.materialCode ?: "",
            weightKg = Math.round(tx.weightKg * 100.0) / 100.0,
            finalPayout = Math.round(tx.finalPayout * 100.0) / 100.0,
            latitude = String.format(Locale.US, "%.5f", tx.gpsLatitude).toDoubleOrNull() ?: tx.gpsLatitude,
            longitude = String.format(Locale.US, "%.5f", tx.gpsLongitude).toDoubleOrNull() ?: tx.gpsLongitude,
            nonce = tx.nonce ?: "",
            epochMillis = tx.epochMillis,
            voiceTranscript = truncatedTranscript
        )
        return gson.toJson(payload)
    }

    fun deserialize(json: String): CompactSubmissionPayload {
        return gson.fromJson(json, CompactSubmissionPayload::class.java)
    }

    fun getPayloadSizeBytes(json: String): Int {
        return json.toByteArray(Charsets.UTF_8).size
    }

    fun isWithinBandwidthConstraint(json: String): Boolean {
        return getPayloadSizeBytes(json) <= MAX_ALLOWED_PAYLOAD_BYTES
    }
}
