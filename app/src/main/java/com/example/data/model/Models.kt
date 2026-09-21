package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class SubscriptionTier(
    val title: String,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val dailyScans: Int,
    val chatTokensPerDay: Int, // -1 means unlimited
    val hasAds: Boolean,
    val description: String
) {
    FREE(
        title = "Free (Ad-Supported)",
        monthlyPrice = 0.0,
        yearlyPrice = 0.0,
        dailyScans = 3,
        chatTokensPerDay = 5,
        hasAds = true,
        description = "3 AI diagnostic scans & 5 Gemini real-time chat sessions per day. Ad-supported."
    ),
    PREMIUM(
        title = "Premium",
        monthlyPrice = 4.99,
        yearlyPrice = 49.99,
        dailyScans = 15,
        chatTokensPerDay = 35,
        hasAds = false,
        description = "15 scans/day, 35 real-time Gemini live-chat sessions per day, ad-free, advanced personalized route engine & lane assist."
    ),
    PRO(
        title = "PRO Max",
        monthlyPrice = 8.99,
        yearlyPrice = 84.99,
        dailyScans = 50,
        chatTokensPerDay = -1, // Unlimited
        hasAds = false,
        description = "Unlimited real-time Gemini live-chat, 50 scans/day, freeze-frame ECU diagnostics, multi-stop optimization & priority telemetry."
    )
}

enum class BillingPeriod {
    MONTHLY,
    YEARLY
}

data class UserAccount(
    val uid: String = "guest_user",
    val email: String? = null,
    val displayName: String = "Driver",
    val photoUrl: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val scansUsedToday: Int = 0,
    val chatTokensUsedToday: Int = 0,
    val lastResetDate: String = "",
    val isAnonymous: Boolean = true
)

enum class ScanSeverity {
    HEALTHY,
    ADVISORY,
    MODERATE,
    CRITICAL
}

data class DtcCode(
    val code: String,
    val description: String,
    val system: String,
    val severity: ScanSeverity,
    val symptoms: String,
    val possibleCauses: String,
    val estimatedRepairCost: String,
    val recommendedFix: String
)

@Entity(tableName = "diagnostic_scans")
data class DiagnosticScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val vehicleName: String,
    val vin: String,
    val healthScore: Int,
    val severity: String,
    val dtcCodesJson: String,
    val aiSummary: String,
    val estimatedTotalRepairCost: String,
    val milStatus: Boolean,
    val rpm: Int,
    val coolantTemp: Int,
    val batteryVoltage: Float
)

@Entity(tableName = "vehicle_profiles")
data class VehicleProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String = "Touring Elite Edition",
    val vin: String = "1HGCV1F34PA092811",
    val engine: String = "2.0L Turbo 4-Cyl DOHC 16V",
    val size: String = "Mid-Size Sedan (196.1″ L × 73.3″ W × 57.1″ H)",
    val bodyType: String = "4-Door Sedan / 5-Passenger",
    val transmission: String = "10-Speed Electronic Sport Automatic",
    val driveType: String = "Intelligent All-Wheel Drive (AWD)",
    val horsepower: String = "252 hp @ 6,500 RPM",
    val torque: String = "273 lb-ft @ 1,500–4,000 RPM",
    val fuelType: String = "Gasoline (Premium 91+ Unleaded)",
    val tankCapacity: String = "14.8 Gallons (~470 mi Range)",
    val curbWeight: String = "3,528 lbs (GVWR: 4,560 lbs)",
    val tireSpec: String = "235/40R19 96V (33 PSI Cold)",
    val towingCapacity: String = "1,500 lbs max",
    val obdProtocol: String = "ISO 15765-4 CAN (11-bit / 500 kbaud)",
    val ecuFirmware: String = "ECU-CAL-v4.8.2-HON",
    val oilSpec: String = "0W-20 Full Synthetic (4.4 qt)",
    val coolantSpec: String = "OEM Long Life Type 2 (6.8 qt)",
    val brakeFluidSpec: String = "DOT 4 Heavy Duty Synthetic",
    val mileage: Int = 45200,
    val isDefault: Boolean = true
)

@Entity(tableName = "maintenance_tasks")
data class MaintenanceTask(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val intervalMiles: Int,
    val intervalMonths: Int,
    val lastCompletedMiles: Int,
    val lastCompletedDate: Long,
    val notes: String = ""
)

@Entity(tableName = "damage_events")
data class DamageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val component: String,
    val severity: String,
    val description: String,
    val gForceImpact: Float = 1.2f,
    val routeContext: String = "Highway Bypass"
)

@Entity(tableName = "trip_logs")
data class TripLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val origin: String,
    val destination: String,
    val distanceMiles: Double,
    val avgSpeedMph: Double,
    val avgMpg: Double,
    val harshBrakingCount: Int = 0,
    val obdFaultsDetected: Int = 0,
    val damageRiskScore: Int = 94
)

data class DamageScoreReport(
    val overallScore: Int = 94,
    val powertrainScore: Int = 98,
    val suspensionScore: Int = 92,
    val exhaustScore: Int = 88,
    val brakeScore: Int = 94,
    val wearAssessment: String = "Minimal structural wear. Suspension damping within optimal bounds. Minor exhaust manifold heat cycle stress noted.",
    val recommendedRouteAdvice: String = "Recommending smooth asphalt bypass (Highway 101) to minimize chassis vibration."
)

enum class RouteSuggestionType(val label: String, val badge: String, val iconName: String) {
    FASTEST("Fastest Route", "Dynamic Bypass", "Speed"),
    ECO_FRIENDLY("Eco-Logic Route", "Save Fuel 14%", "Eco"),
    SCENIC_SMOOTH("Scenic & Smooth", "Low Wear & Tear", "Landscape"),
    HEAVY_VEHICLE("Commercial / Cargo", "Clearance Safe", "LocalShipping")
}

data class TrafficIncident(
    val id: String,
    val type: String, // Accident, Congestion, Construction, Hazard, Speed Trap
    val title: String,
    val description: String,
    val location: String,
    val delayMinutes: Int,
    val severity: String // Warning, Critical, Info
)

data class RouteOption(
    val id: String,
    val name: String,
    val type: RouteSuggestionType,
    val distanceMiles: Double,
    val durationMinutes: Int,
    val trafficDelayMinutes: Int,
    val trafficLevel: String, // Normal, Moderate, Heavy, Severe
    val tollCost: Double,
    val fuelSavingsPercent: Int,
    val co2SavedKg: Double,
    val summary: String,
    val personalizedReason: String,
    val waypoints: List<String>,
    val incidents: List<TrafficIncident>
)

@Entity(tableName = "saved_routes")
data class SavedRouteRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val origin: String,
    val destination: String,
    val routeTypeName: String,
    val distanceMiles: Double,
    val durationMinutes: Int,
    val fuelSaved: String
)

data class LiveTelemetry(
    val rpm: Int = 1850,
    val speedMph: Int = 45,
    val coolantTempF: Int = 195,
    val intakeAirTempF: Int = 82,
    val fuelRailPressurePsi: Int = 2150,
    val throttlePercent: Int = 24,
    val o2Sensor1Volt: Float = 0.78f,
    val batteryVoltage: Float = 14.2f,
    val massAirFlowGps: Float = 12.4f,
    val engineLoadPercent: Int = 32,
    val fuelEconomyMpg: Float = 28.5f,
    val transmissionTempF: Int = 165,
    val isConnected: Boolean = true,
    val milCheckEngineOn: Boolean = false
)

enum class ChatSender {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class GeminiAiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String
) {
    LIVE_3_8(
        id = "gemini-3.8-live",
        displayName = "gemini-3.8-live",
        description = "Next-gen Live API for real-time bidirectional voice conversation",
        badge = "Live API"
    ),
    LIVE_VOICE(
        id = "gemini-2.5-flash-native-audio-preview-12-2025",
        displayName = "gemini-2.5-flash-native-audio",
        description = "Native audio preview for conversational driving co-pilot",
        badge = "Gemini Audio"
    ),
    FLASH_3_8(
        id = "models/gemini-3.8-flash",
        displayName = "models/gemini-3.8-flash",
        description = "Next-gen multimodal intelligence with high-speed natural response generation",
        badge = "Gemini 3.8 Flash"
    ),
    FLASH(
        id = "gemini-3.5-flash",
        displayName = "gemini-3.5-flash",
        description = "Standard Gemini model for general tasks & real-time chat with Google Search/Maps",
        badge = "General Tasks"
    ),
    PRO_PREVIEW(
        id = "gemini-3.1-pro-preview",
        displayName = "gemini-3.1-pro-preview",
        description = "Advanced reasoning for particularly complex tasks & deep diagnostics",
        badge = "Complex Tasks"
    ),
    FLASH_LITE(
        id = "gemini-3.1-flash-lite",
        displayName = "gemini-3.1-flash-lite",
        description = "Fast-executing lightweight model for tasks that should happen fast",
        badge = "Ultra Fast"
    )
}

enum class ChatbotPersona(
    val title: String,
    val shortName: String,
    val iconName: String,
    val systemInstruction: String
) {
    MECHANIC(
        title = "Master Mechanic & OBD-II Co-Pilot",
        shortName = "Master Mechanic",
        iconName = "Build",
        systemInstruction = "You are a master ASE-certified automotive technician and OBD-II telemetry specialist in GPS-Route-Logic. Diagnose vehicles with 99% accuracy using trip logs and live OBD2 data with fault codes. Always give short, concise answers with emojis 🚗🔧⚡."
    ),
    NAVIGATOR(
        title = "Highway & Live Traffic Navigator",
        shortName = "Traffic Navigator",
        iconName = "Navigation",
        systemInstruction = "You are an automotive GPS real-time route specialist. Recommend alternate routes based on logged trip data, OBD2 data, and damage scores. Always give short, concise answers with emojis 📍🚗⚡."
    ),
    CARGO(
        title = "Heavy Cargo & Fleet Optimization Specialist",
        shortName = "Cargo Specialist",
        iconName = "LocalShipping",
        systemInstruction = "You are a commercial fleet logistics and heavy cargo route optimizer. Recommend safe routes, bridge clearances, and eco routes based on trip logs. Always give short, concise answers with emojis 🚚📦⚡."
    ),
    TUNER(
        title = "High-Performance ECU & Dyno Engineer",
        shortName = "ECU Tuner",
        iconName = "ElectricBolt",
        systemInstruction = "You are a high-performance automotive powertrain and ECU tuning specialist. Analyze boost, AFR, timing curves, and high-performance telemetry. Always give short, concise answers with emojis ⚡🏎️🔧."
    )
}

enum class VeoAspectRatio(val displayName: String, val ratioString: String, val isLandscape: Boolean) {
    LANDSCAPE_16_9("16:9 Landscape", "16:9", true),
    PORTRAIT_9_16("9:16 Portrait", "9:16", false)
}

enum class VeoGenerationStatus {
    GENERATING,
    READY,
    FAILED
}

data class VeoVideoGeneration(
    val id: String,
    val prompt: String,
    val model: String = "veo-3.1-fast-generate-preview",
    val aspectRatio: VeoAspectRatio = VeoAspectRatio.LANDSCAPE_16_9,
    val status: VeoGenerationStatus = VeoGenerationStatus.READY,
    val durationSeconds: Int = 6,
    val videoTitle: String = "Automotive Video Simulation",
    val previewStyle: String = "CYBERPUNK_TELEMETRY",
    val progressPercent: Int = 100,
    val timestamp: Long = System.currentTimeMillis()
)

data class GroundingCitation(
    val title: String,
    val url: String,
    val snippet: String? = null
)

data class GroundedMapPlace(
    val name: String,
    val address: String,
    val rating: Double = 4.7,
    val category: String, // "EV Supercharger", "Certified Mechanic", "Gas Station", "Scenic Rest Area"
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val openStatus: String = "Open Now • 24/7"
)

enum class LiveVoiceSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val actionSuggestion: String? = null,
    val modelUsed: String? = null,
    val personaUsed: String? = null,
    val searchCitations: List<GroundingCitation> = emptyList(),
    val groundedPlaces: List<GroundedMapPlace> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val veoVideo: VeoVideoGeneration? = null
) {
    fun toEntity(): ChatMessageEntity {
        val citationsArr = JSONArray()
        for (c in searchCitations) {
            val obj = JSONObject()
            obj.put("title", c.title)
            obj.put("url", c.url)
            if (c.snippet != null) obj.put("snippet", c.snippet)
            citationsArr.put(obj)
        }

        val placesArr = JSONArray()
        for (p in groundedPlaces) {
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("address", p.address)
            obj.put("rating", p.rating)
            obj.put("category", p.category)
            obj.put("latitude", p.latitude)
            obj.put("longitude", p.longitude)
            obj.put("openStatus", p.openStatus)
            placesArr.put(obj)
        }

        val queriesArr = JSONArray()
        for (q in searchQueries) {
            queriesArr.put(q)
        }

        val veoJsonStr = veoVideo?.let { v ->
            val vObj = JSONObject()
            vObj.put("id", v.id)
            vObj.put("prompt", v.prompt)
            vObj.put("model", v.model)
            vObj.put("aspectRatio", v.aspectRatio.name)
            vObj.put("status", v.status.name)
            vObj.put("durationSeconds", v.durationSeconds)
            vObj.put("videoTitle", v.videoTitle)
            vObj.put("previewStyle", v.previewStyle)
            vObj.put("progressPercent", v.progressPercent)
            vObj.put("timestamp", v.timestamp)
            vObj.toString()
        }

        return ChatMessageEntity(
            id = id,
            sender = sender.name,
            content = content,
            timestamp = timestamp,
            isVoice = isVoice,
            actionSuggestion = actionSuggestion,
            modelUsed = modelUsed,
            personaUsed = personaUsed,
            citationsJson = citationsArr.toString(),
            placesJson = placesArr.toString(),
            searchQueriesJson = queriesArr.toString(),
            veoJson = veoJsonStr
        )
    }
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val actionSuggestion: String? = null,
    val modelUsed: String? = null,
    val personaUsed: String? = null,
    val citationsJson: String = "[]",
    val placesJson: String = "[]",
    val searchQueriesJson: String = "[]",
    val veoJson: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val parsedCitations = mutableListOf<GroundingCitation>()
        try {
            val arr = JSONArray(citationsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsedCitations.add(
                    GroundingCitation(
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        snippet = if (obj.has("snippet")) obj.optString("snippet") else null
                    )
                )
            }
        } catch (_: Throwable) {}

        val parsedPlaces = mutableListOf<GroundedMapPlace>()
        try {
            val arr = JSONArray(placesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsedPlaces.add(
                    GroundedMapPlace(
                        name = obj.optString("name", ""),
                        address = obj.optString("address", ""),
                        rating = obj.optDouble("rating", 4.7),
                        category = obj.optString("category", "Automotive"),
                        latitude = obj.optDouble("latitude", 37.7749),
                        longitude = obj.optDouble("longitude", -122.4194),
                        openStatus = obj.optString("openStatus", "Open")
                    )
                )
            }
        } catch (_: Throwable) {}

        val parsedQueries = mutableListOf<String>()
        try {
            val arr = JSONArray(searchQueriesJson)
            for (i in 0 until arr.length()) {
                parsedQueries.add(arr.getString(i))
            }
        } catch (_: Throwable) {}

        val parsedVeo = veoJson?.let { str ->
            try {
                val obj = JSONObject(str)
                val aspect = try {
                    VeoAspectRatio.valueOf(obj.optString("aspectRatio", "LANDSCAPE_16_9"))
                } catch (_: Throwable) {
                    VeoAspectRatio.LANDSCAPE_16_9
                }
                val stat = try {
                    VeoGenerationStatus.valueOf(obj.optString("status", "READY"))
                } catch (_: Throwable) {
                    VeoGenerationStatus.READY
                }
                VeoVideoGeneration(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    prompt = obj.optString("prompt", ""),
                    model = obj.optString("model", "veo-3.1-fast-generate-preview"),
                    aspectRatio = aspect,
                    status = stat,
                    durationSeconds = obj.optInt("durationSeconds", 6),
                    videoTitle = obj.optString("videoTitle", "Automotive Video Simulation"),
                    previewStyle = obj.optString("previewStyle", "CYBERPUNK_TELEMETRY"),
                    progressPercent = obj.optInt("progressPercent", 100),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (_: Throwable) {
                null
            }
        }

        val chatSender = try {
            ChatSender.valueOf(sender)
        } catch (_: Throwable) {
            ChatSender.ASSISTANT
        }

        return ChatMessage(
            id = id,
            sender = chatSender,
            content = content,
            timestamp = timestamp,
            isVoice = isVoice,
            actionSuggestion = actionSuggestion,
            modelUsed = modelUsed,
            personaUsed = personaUsed,
            searchCitations = parsedCitations,
            groundedPlaces = parsedPlaces,
            searchQueries = parsedQueries,
            veoVideo = parsedVeo
        )
    }
}

data class FirestoreSyncState(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val syncSuccess: Boolean = true,
    val message: String = "Linked to Firestore project: gps-route-logic"
)
