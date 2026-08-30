package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
        description = "3 AI diagnostic scans & basic GPS traffic routing per day. Ad-supported."
    ),
    PREMIUM(
        title = "Premium",
        monthlyPrice = 4.99,
        yearlyPrice = 49.99,
        dailyScans = 10,
        chatTokensPerDay = 25,
        hasAds = false,
        description = "10 scans/day, 25 live chat tokens, ad-free, advanced personalized route engine & lane assist."
    ),
    PRO(
        title = "PRO Max",
        monthlyPrice = 9.99,
        yearlyPrice = 89.99,
        dailyScans = 50,
        chatTokensPerDay = -1, // Unlimited
        hasAds = false,
        description = "Unlimited live voice co-pilot chat, 50 scans/day, freeze-frame ECU diagnostics, multi-stop optimization, & full maintenance logs."
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
    PRO_PREVIEW(
        id = "gemini-3.1-pro-preview",
        displayName = "gemini-3.1-pro-preview",
        description = "Advanced reasoning for complex ECU diagnostics & multi-stop route mathematics",
        badge = "Complex Tasks"
    ),
    FLASH(
        id = "gemini-3.5-flash",
        displayName = "gemini-3.5-flash",
        description = "Balanced intelligence with Google Search & Google Maps Grounding",
        badge = "General & Grounded"
    ),
    FLASH_LITE(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "gemini-3.1-flash-lite",
        description = "High-speed latency for instant quick answers & DTC definitions",
        badge = "Ultra Fast"
    ),
    LIVE_VOICE(
        id = "gemini-3.1-flash-live-preview",
        displayName = "gemini-3.1-flash-live-preview (Live API)",
        description = "Real-time bidirectional audio & conversational voice co-pilot",
        badge = "Live Voice API"
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
        systemInstruction = "You are a master ASE-certified automotive technician and OBD-II telemetry specialist in GPS-Route-Logic. Explain diagnostic trouble codes (DTCs), sensor freeze frames, fuel trim, ignition timing, and component failure symptoms clearly with step-by-step troubleshooting guides and realistic repair cost estimates."
    ),
    NAVIGATOR(
        title = "Highway & Live Traffic Navigator",
        shortName = "Traffic Navigator",
        iconName = "Navigation",
        systemInstruction = "You are an expert GPS highway navigator and dynamic traffic controller for GPS-Route-Logic. Help drivers navigate real-time traffic jams, calculate optimal bypass detours, compare toll vs non-toll alternatives, and provide lane-level guidance tailored to weather conditions and peak commuting hours."
    ),
    CARGO(
        title = "Heavy Cargo & Fleet Optimization Specialist",
        shortName = "Cargo Specialist",
        iconName = "LocalShipping",
        systemInstruction = "You are a commercial fleet logistics and heavy cargo route optimizer for GPS-Route-Logic. Assist truck drivers and cargo haulers with bridge clearance heights, gross vehicle weight limits, steep grade brake heat prevention, eco-fuel optimization, and DOT rest-stop planning."
    )
}

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
    val searchQueries: List<String> = emptyList()
)

data class FirestoreSyncState(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val syncSuccess: Boolean = true,
    val message: String = "Linked to Firestore project: gps-route-logic"
)
