package com.example.sihscrap.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ConditionAssessmentDto(
    @SerializedName("wear_grade") val wearGrade: String = "Grade B",
    @SerializedName("casing_intactness_pct") val casingIntactnessPct: Double = 70.0,
    @SerializedName("oxidation_rust_pct") val oxidationRustPct: Double = 10.0,
    @SerializedName("purity_factor") val purityFactor: Double = 0.85,
    @SerializedName("damage_observations") val damageObservations: List<String> = emptyList()
)

data class HazardSafetyAlertDto(
    @SerializedName("has_toxic_hazards") val hasToxicHazards: Boolean = false,
    @SerializedName("hazard_level") val hazardLevel: String = "LOW",
    @SerializedName("toxic_substances") val toxicSubstances: List<String> = emptyList(),
    @SerializedName("alert_en") val alertEn: String = "Safe to handle.",
    @SerializedName("alert_hi") val alertHi: String = "सुरक्षित सामग्री।",
    @SerializedName("alert_mr") val alertMr: String = "सुरक्षित भंगार.",
    @SerializedName("safe_handling_protocol") val safeHandlingProtocol: String = "Handle normally."
)

data class ScrapValuationQuoteDto(
    @SerializedName("material_code") val materialCode: String = "e_waste_general",
    @SerializedName("material_name") val materialName: String = "General E-Waste",
    @SerializedName("base_mandi_rate_inr_per_kg") val baseMandiRateInrPerKg: Double = 75.0,
    @SerializedName("estimated_weight_range_kg") val estimatedWeightRangeKg: List<Double> = listOf(1.0, 3.0),
    @SerializedName("estimated_payout_range_inr") val estimatedPayoutRangeInr: List<Double> = listOf(60.0, 180.0),
    @SerializedName("carbon_offset_kg") val carbonOffsetKg: Double = 5.0
)

data class MultimodalAnalysisResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("item_name") val itemName: String = "E-Waste Scrap",
    @SerializedName("item_name_hi") val itemNameHi: String = "ई-कचरा",
    @SerializedName("item_name_mr") val itemNameMr: String = "ई-भंगार",
    @SerializedName("cpcb_category") val cpcbCategory: String = "e_waste",
    @SerializedName("condition") val condition: ConditionAssessmentDto = ConditionAssessmentDto(),
    @SerializedName("safety_hazard") val safetyHazard: HazardSafetyAlertDto = HazardSafetyAlertDto(),
    @SerializedName("valuation") val valuation: ScrapValuationQuoteDto = ScrapValuationQuoteDto(),
    @SerializedName("authorized_recycler_channel") val authorizedRecyclerChannel: String = "CPCB Recycler",
    @SerializedName("ai_engine") val aiEngine: String = "Zero-Shot Multimodal VLM"
)


data class RateResponse(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("current_spot_rate") val current_spot_rate: Double,
    @SerializedName("name_hi") val nameHi: String? = null,
    @SerializedName("name_mr") val nameMr: String? = null,
    @SerializedName("category_code") val categoryCode: String? = null,
    @SerializedName("default_purity") val defaultPurity: Double? = 1.0,
    @SerializedName("epr_bonus") val eprBonus: Double? = 5.0
)

data class TransactionSubmissionRequest(
    @SerializedName("receipt_hash") val receiptHash: String,
    @SerializedName("material_code") val materialCode: String,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("purity_factor") val purityFactor: Double = 1.0,
    @SerializedName("calculated_price") val calculatedPrice: Double,
    @SerializedName("pickup_latitude") val pickupLatitude: Double,
    @SerializedName("pickup_longitude") val pickupLongitude: Double,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("epoch_millis") val epochMillis: Long,
    @SerializedName("voice_input_transcript") val voiceInputTranscript: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class TransactionSubmissionResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("id") val id: Long = 0,
    @SerializedName("transaction_id") val transactionId: Long? = null,
    @SerializedName("status") val status: String = "accepted",
    @SerializedName("receipt_hash") val receiptHash: String? = null,
    @SerializedName("message") val message: String? = null
)

data class RecyclerOut(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("regulatory_board") val regulatoryBoard: String? = "CPCB / MPCB",
    @SerializedName("contact_phone") val contactPhone: String? = "+91 20 2740 0000",
    @SerializedName("operating_hours") val operatingHours: String? = "08:00 AM - 06:00 PM",
    @SerializedName("accepted_category_codes") val acceptedCategories: String? = "e_waste, non_ferrous, ferrous"
)

data class NearestRecyclerItem(
    @SerializedName("recycler") val recycler: RecyclerOut,
    @SerializedName("distance_km") val distance_km: Double,
    @SerializedName("google_maps_directions_url") val google_maps_directions_url: String,
    @SerializedName("estimated_driving_time_mins") val estimatedDrivingTimeMins: Int? = 30
)

data class NearestRecyclersResponse(
    @SerializedName("nearby_recyclers") val nearby_recyclers: List<NearestRecyclerItem> = emptyList(),
    @SerializedName("total_found") val totalFound: Int = 0
)

data class DuressAlertRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("epoch_millis") val epochMillis: Long,
    @SerializedName("trigger_code") val triggerCode: String,
    @SerializedName("battery_level_pct") val batteryLevelPct: Int = 85,
    @SerializedName("is_silent") val isSilent: Boolean = true,
    @SerializedName("telemetry") val telemetry: String = "ERSS_112_SOS_TRIGGER"
)

data class DuressAlertResponse(
    @SerializedName("status") val status: String = "dispatched",
    @SerializedName("alert_id") val alertId: String = "SOS-ERSS-911",
    @SerializedName("decoy_mode_active") val decoyModeActive: Boolean = true,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

interface BackendApi {
    // 1. Live Rates (matches /api/v1/rates/live with fallback to /api/v1/rates)
    @GET("/api/v1/rates/live")
    suspend fun getLiveRates(): List<RateResponse>

    @GET("/api/v1/rates")
    suspend fun getLiveRatesFallback(): List<RateResponse>

    // 2. Transaction Submission (matches /api/v1/transactions/submit with fallback to /api/v1/transactions/)
    @POST("/api/v1/transactions/submit")
    suspend fun submitTransaction(@Body req: TransactionSubmissionRequest): TransactionSubmissionResponse

    @POST("/api/v1/transactions/")
    suspend fun submitTransactionFallback(@Body req: TransactionSubmissionRequest): TransactionSubmissionResponse

    // 3. Nearby Facilities (matches /api/v1/facilities/nearby with fallback to /api/v1/recyclers/nearest)
    @GET("/api/v1/facilities/nearby")
    suspend fun getNearbyFacilities(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius_km") radiusKm: Double = 50.0,
        @Query("limit") limit: Int = 10
    ): NearestRecyclersResponse

    @GET("/api/v1/recyclers/nearest")
    suspend fun getNearestRecyclers(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): NearestRecyclersResponse

    // 4. Security Duress SOS Alert
    @POST("/api/v1/security/duress-alert")
    suspend fun sendDuressAlert(@Body req: DuressAlertRequest): DuressAlertResponse

    // 5. Option B: Multimodal Vision, Condition Assessment & Hazard Analysis
    @Multipart
    @POST("/api/v1/vision/analyze-multimodal")
    suspend fun analyzeMultimodal(
        @Part file: MultipartBody.Part
    ): MultimodalAnalysisResponse
}


object RetrofitClient {
    private var baseUrl = "http://192.168.88.8:8000"

    fun setBaseUrl(url: String) {
        baseUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        _instance = null
    }

    fun getBaseUrl(): String = baseUrl

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private var _instance: BackendApi? = null

    val instance: BackendApi
        get() {
            if (_instance == null) {
                _instance = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BackendApi::class.java)
            }
            return _instance!!
        }
}
