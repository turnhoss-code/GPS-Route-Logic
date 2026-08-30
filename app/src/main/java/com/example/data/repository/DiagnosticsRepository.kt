package com.example.data.repository

import com.example.data.local.DamageDao
import com.example.data.local.MaintenanceDao
import com.example.data.local.ScanDao
import com.example.data.local.TripLogDao
import com.example.data.local.VehicleDao
import com.example.data.model.DamageEvent
import com.example.data.model.DamageScoreReport
import com.example.data.model.DiagnosticScanRecord
import com.example.data.model.DtcCode
import com.example.data.model.LiveTelemetry
import com.example.data.model.MaintenanceTask
import com.example.data.model.ScanSeverity
import com.example.data.model.TripLog
import com.example.data.model.VehicleProfile
import com.example.data.remote.GeminiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class DiagnosticsRepository(
    private val scanDao: ScanDao,
    private val vehicleDao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val damageDao: DamageDao,
    private val tripLogDao: TripLogDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dtcListAdapter = moshi.adapter<List<DtcCode>>(
        Types.newParameterizedType(List::class.java, DtcCode::class.java)
    )

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _damageReport = MutableStateFlow(DamageScoreReport())
    val damageReport: StateFlow<DamageScoreReport> = _damageReport.asStateFlow()

    val scanHistory: Flow<List<DiagnosticScanRecord>> = scanDao.getAllScans()
    val vehicles: Flow<List<VehicleProfile>> = vehicleDao.getAllVehicles()
    val maintenanceTasks: Flow<List<MaintenanceTask>> = maintenanceDao.getAllTasks()
    val damageEvents: Flow<List<DamageEvent>> = damageDao.getAllDamageEvents()
    val tripLogs: Flow<List<TripLog>> = tripLogDao.getAllTrips()

    // Sample OBD-II database for DTC engine
    val sampleDtcDatabase = listOf(
        DtcCode(
            code = "P0300",
            description = "Random / Multiple Cylinder Misfire Detected",
            system = "Powertrain / Ignition",
            severity = ScanSeverity.CRITICAL,
            symptoms = "Engine stutter under load, rough idle, blinking Check Engine Light (MIL).",
            possibleCauses = "Worn spark plugs, failing ignition coil on Cyl #2/#4, clogged fuel injectors, or intake manifold vacuum leak.",
            estimatedRepairCost = "$120 - $350 (Coils & Plugs)",
            recommendedFix = "Inspect spark plug electrode gaps and swap coil #2 with #1 to isolate misfire cylinder."
        ),
        DtcCode(
            code = "P0420",
            description = "Catalyst System Efficiency Below Threshold (Bank 1)",
            system = "Exhaust / Emissions",
            severity = ScanSeverity.MODERATE,
            symptoms = "Reduced high-RPM power, mild sulfur exhaust smell, fuel trim variance.",
            possibleCauses = "Aged catalytic converter, faulty downstream O2 sensor, exhaust manifold gasket leak.",
            estimatedRepairCost = "$250 - $950",
            recommendedFix = "Verify downstream O2 sensor voltage waveform (0.45V steady). Inspect for exhaust pinhole leaks before catalytic converter replacement."
        ),
        DtcCode(
            code = "P0171",
            description = "System Too Lean (Bank 1)",
            system = "Fuel & Air Metering",
            severity = ScanSeverity.MODERATE,
            symptoms = "Hesitation on throttle tip-in, high positive fuel trim (+18%), pinging under load.",
            possibleCauses = "Dirty Mass Air Flow (MAF) sensor, PCV valve hose crack, low fuel pump pressure.",
            estimatedRepairCost = "$45 - $220",
            recommendedFix = "Clean MAF sensor with dedicated MAF spray cleaner and perform smoke test on vacuum intake."
        ),
        DtcCode(
            code = "P0128",
            description = "Coolant Thermostat (Coolant Temp Below Regulating Temperature)",
            system = "Engine Cooling",
            severity = ScanSeverity.ADVISORY,
            symptoms = "Engine takes long time to reach warm operating temp, cabin heater lukewarm in winter.",
            possibleCauses = "Thermostat stuck open, coolant temperature sensor calibration drift.",
            estimatedRepairCost = "$80 - $190",
            recommendedFix = "Replace engine thermostat and flush coolant fluid."
        ),
        DtcCode(
            code = "P0455",
            description = "Evaporative Emission System Leak Detected (Gross Leak / No Flow)",
            system = "EVAP Emissions",
            severity = ScanSeverity.ADVISORY,
            symptoms = "Gas vapor odor near filler neck, EVAP monitor incomplete.",
            possibleCauses = "Loose/defective fuel tank cap, EVAP purge solenoid stuck, filler neck seal torn.",
            estimatedRepairCost = "$20 - $140",
            recommendedFix = "Tighten or replace fuel cap rubber O-ring seal, test purge valve duty cycle."
        )
    )

    fun updateTelemetrySimulated(deltaRpm: Int = 0, deltaSpeed: Int = 0) {
        val current = _telemetry.value
        val newRpm = (current.rpm + deltaRpm + Random.nextInt(-30, 35)).coerceIn(750, 6500)
        val newSpeed = (current.speedMph + deltaSpeed + Random.nextInt(-1, 2)).coerceIn(0, 110)
        val newCoolant = (current.coolantTempF + Random.nextInt(-1, 2)).coerceIn(180, 225)
        val newLoad = (newRpm / 65).coerceIn(15, 95)
        val newMpg = (32.0f - (newSpeed * 0.1f) + (Random.nextFloat() * 2 - 1)).coerceIn(14.0f, 44.0f)
        val newVoltage = 14.1f + (Random.nextFloat() * 0.3f - 0.15f)

        _telemetry.value = current.copy(
            rpm = newRpm,
            speedMph = newSpeed,
            coolantTempF = newCoolant,
            engineLoadPercent = newLoad,
            fuelEconomyMpg = newMpg,
            batteryVoltage = ((newVoltage * 10).toInt() / 10.0f)
        )
    }

    suspend fun performAiDiagnosticScan(
        vehicleName: String = "2024 Honda Accord Touring",
        vin: String = "1HGCV1F34PA092811",
        deepScan: Boolean = false
    ): DiagnosticScanRecord {
        _isScanning.value = true
        delay(1600) // Scan telemetry handshake simulation

        val foundCodes = if (Random.nextInt(100) < 65) {
            if (deepScan) {
                listOf(sampleDtcDatabase[0], sampleDtcDatabase[1])
            } else {
                listOf(sampleDtcDatabase[0])
            }
        } else {
            emptyList()
        }

        val healthScore = if (foundCodes.isEmpty()) 98 else if (foundCodes.any { it.severity == ScanSeverity.CRITICAL }) 64 else 82
        val severityStr = if (foundCodes.isEmpty()) "HEALTHY" else if (foundCodes.any { it.severity == ScanSeverity.CRITICAL }) "CRITICAL" else "MODERATE"
        val totalCost = if (foundCodes.isEmpty()) "$0.00" else "$240 - $550"

        val prompt = "Analyze the following OBD-II vehicle scan: Vehicle: $vehicleName, DTCs: ${foundCodes.map { "${it.code}: ${it.description}" }.joinToString("; ")}. Provide a 2-sentence expert diagnostic summary and priority action."
        val aiSummary = GeminiClient.generateAiText(
            prompt = prompt,
            systemInstruction = "You are GPS Route Logic's expert ASE-Certified Master Diagnostic AI. Be concise, highly professional, and safety-oriented."
        )

        val record = DiagnosticScanRecord(
            vehicleName = vehicleName,
            vin = vin,
            healthScore = healthScore,
            severity = severityStr,
            dtcCodesJson = dtcListAdapter.toJson(foundCodes),
            aiSummary = aiSummary,
            estimatedTotalRepairCost = totalCost,
            milStatus = foundCodes.isNotEmpty(),
            rpm = _telemetry.value.rpm,
            coolantTemp = _telemetry.value.coolantTempF,
            batteryVoltage = _telemetry.value.batteryVoltage
        )

        val id = scanDao.insertScan(record)
        _isScanning.value = false
        return record.copy(id = id)
    }

    suspend fun clearCodes() {
        val current = _telemetry.value
        _telemetry.value = current.copy(milCheckEngineOn = false)
    }

    fun parseDtcCodes(json: String): List<DtcCode> {
        return try {
            dtcListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addVehicle(profile: VehicleProfile): Long {
        return vehicleDao.insertVehicle(profile)
    }

    suspend fun updateVehicle(profile: VehicleProfile) {
        vehicleDao.insertVehicle(profile)
    }

    suspend fun deleteVehicle(id: Long) {
        vehicleDao.deleteVehicle(id)
    }

    suspend fun setDefaultVehicle(id: Long) {
        vehicleDao.clearDefaultVehicles()
        vehicleDao.setDefaultVehicle(id)
    }

    suspend fun markMaintenanceTaskCompleted(id: String, currentMileage: Int) {
        maintenanceDao.markCompleted(id, currentMileage, System.currentTimeMillis())
    }

    suspend fun insertMaintenanceTask(task: MaintenanceTask) {
        maintenanceDao.insertTask(task)
    }

    suspend fun initDefaultMaintenanceIfEmpty() {
        val defaultTasks = listOf(
            MaintenanceTask(
                id = "1",
                name = "Synthetic Oil Change",
                type = "oil_change",
                intervalMiles = 5000,
                intervalMonths = 6,
                lastCompletedMiles = 40000,
                lastCompletedDate = 1774964832218L,
                notes = "0W-20 Full Synthetic Oil + OEM Filter"
            ),
            MaintenanceTask(
                id = "2",
                name = "Tire Rotation",
                type = "tire_rotation",
                intervalMiles = 6000,
                intervalMonths = 6,
                lastCompletedMiles = 42000,
                lastCompletedDate = 1782740832218L,
                notes = "Cross-pattern rotation & 35 PSI cold calibration"
            )
        )
        maintenanceDao.insertAll(defaultTasks)
    }

    suspend fun logTrip(trip: TripLog) {
        tripLogDao.insertTrip(trip)
    }

    suspend fun recordDamageEvent(event: DamageEvent) {
        damageDao.insertEvent(event)
    }

    fun deleteScan(id: Long) {
        // can be called from viewModel
    }

    suspend fun removeScan(id: Long) {
        scanDao.deleteScan(id)
    }

    suspend fun initDefaultVehicleIfEmpty() {
        // Will check in ViewModel on init
    }
}
