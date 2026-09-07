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

    // Comprehensive OBD-II database for DTC engine & Code Lookup Screen
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
        ),
        DtcCode(
            code = "P0113",
            description = "Intake Air Temperature Sensor 1 Circuit High Input",
            system = "Fuel & Air Metering",
            severity = ScanSeverity.ADVISORY,
            symptoms = "Hard starting in cold weather, rich fuel trim, Check Engine Light on.",
            possibleCauses = "Unplugged or faulty IAT sensor, open in ground circuit, corroded connector pins.",
            estimatedRepairCost = "$35 - $110",
            recommendedFix = "Check IAT sensor resistance with multimeter; clean pins or replace sensor module."
        ),
        DtcCode(
            code = "P0500",
            description = "Vehicle Speed Sensor 'A' Circuit Malfunction",
            system = "Vehicle Speed & Idle",
            severity = ScanSeverity.MODERATE,
            symptoms = "Erratic or non-functional speedometer, harsh transmission shifts, ABS warning illuminated.",
            possibleCauses = "Faulty transmission output speed sensor, damaged wiring harness, ECU connector corrosion.",
            estimatedRepairCost = "$90 - $210",
            recommendedFix = "Inspect VSS wire harness for chafing near transaxle, probe AC signal output during rotation."
        ),
        DtcCode(
            code = "P0700",
            description = "Transmission Control System Malfunction (MIL Request)",
            system = "Automatic Transmission",
            severity = ScanSeverity.CRITICAL,
            symptoms = "Transmission in limp-home mode (locked in 3rd gear), delayed torque converter lockup.",
            possibleCauses = "TCM detected sub-fault code, low ATF fluid pressure, solenoid pack connector moisture.",
            estimatedRepairCost = "$150 - $600",
            recommendedFix = "Interrogate TCM sub-codes with OBD2 scanner, check transmission fluid level and condition."
        ),
        DtcCode(
            code = "C0035",
            description = "Left Front Wheel Speed Sensor Circuit Fault",
            system = "Chassis / ABS & Traction",
            severity = ScanSeverity.MODERATE,
            symptoms = "ABS light on, Traction Control disabled, pedal pulsation during normal stops.",
            possibleCauses = "Damaged magnetic tone ring, road debris on sensor tip, broken sensor wire pigtail.",
            estimatedRepairCost = "$75 - $180",
            recommendedFix = "Clean tone ring with brake cleaner; test wheel speed sensor millivolt AC wave on scanner."
        ),
        DtcCode(
            code = "C0040",
            description = "Right Front Wheel Speed Sensor Circuit Malfunction",
            system = "Chassis / ABS & Traction",
            severity = ScanSeverity.MODERATE,
            symptoms = "Stabilitrak / VSC warning lamp lit, cruise control unavailable.",
            possibleCauses = "Hub bearing play damaging sensor tip, wire harness severed by suspension arm.",
            estimatedRepairCost = "$80 - $195",
            recommendedFix = "Inspect wheel bearing end-play and replace right front speed sensor assembly."
        ),
        DtcCode(
            code = "C1201",
            description = "Engine Control System Malfunction (ABS / VSC Fail-Safe Mode)",
            system = "Chassis / Brake Integration",
            severity = ScanSeverity.ADVISORY,
            symptoms = "VSC and Trac lights lit concurrently with Check Engine Light.",
            possibleCauses = "Skid control ECU enters fail-safe whenever ECM has active powertrain DTC.",
            estimatedRepairCost = "$0 (Secondary fault)",
            recommendedFix = "Resolve primary engine DTCs (e.g. P0300 or P0171); C1201 clears automatically upon engine fix."
        ),
        DtcCode(
            code = "B0001",
            description = "Driver Frontal Stage 1 Deployment Control (Airbag Loop)",
            system = "Body / Safety Restraints",
            severity = ScanSeverity.CRITICAL,
            symptoms = "Supplemental Restraint System (SRS) airbag light illuminated continuously.",
            possibleCauses = "Clockspring ribbon cable broken inside steering column, loose yellow SRS connector under seat.",
            estimatedRepairCost = "$180 - $420",
            recommendedFix = "Replace steering column clockspring assembly with battery disconnected for >15 minutes."
        ),
        DtcCode(
            code = "B1000",
            description = "Electronic Control Unit (ECU) Internal Hardware Fault",
            system = "Body Control Module (BCM)",
            severity = ScanSeverity.CRITICAL,
            symptoms = "Intermittent power window/lock operation, interior lighting anomalies.",
            possibleCauses = "Voltage spike from weak battery, water ingress into BCM junction block, internal microprocessor fault.",
            estimatedRepairCost = "$350 - $850",
            recommendedFix = "Inspect BCM for water corrosion; re-flash firmware calibration or replace BCM."
        ),
        DtcCode(
            code = "B1318",
            description = "Battery Voltage Below Vehicle Operational Minimum (<10.5V)",
            system = "Electrical / Charging",
            severity = ScanSeverity.MODERATE,
            symptoms = "Slow engine crank, flickering dash displays, multiple false-positive sensor DTCs.",
            possibleCauses = "Failing 12V AGM/lead-acid battery, slipping alternator belt, parasitic battery draw when parked.",
            estimatedRepairCost = "$130 - $260",
            recommendedFix = "Perform battery conductance test, check charging voltage (>13.8V running), replace 12V battery."
        ),
        DtcCode(
            code = "U0100",
            description = "Lost Communication With Engine Control Module (ECM / PCM)",
            system = "Network / CAN High Speed Bus",
            severity = ScanSeverity.CRITICAL,
            symptoms = "No-crank / no-start, multiple warning lights on dashboard, transmission shift lock stuck.",
            possibleCauses = "Blown ECM fuse, damaged CAN-H / CAN-L twisted pair wires, disconnected ECM ground wire.",
            estimatedRepairCost = "$110 - $450",
            recommendedFix = "Measure CAN Bus terminating resistance (60 ohms across DLC pins 6 & 14) and verify main power relay."
        ),
        DtcCode(
            code = "U0121",
            description = "Lost Communication With Anti-Lock Brake System (ABS) Module",
            system = "Network / CAN Bus",
            severity = ScanSeverity.MODERATE,
            symptoms = "ABS, Brake, and Stability lights on, speedometer may drop to zero intermittently.",
            possibleCauses = "ABS module power ground broken, corroded ABS wiring harness connector, blown ABS pump fuse.",
            estimatedRepairCost = "$95 - $320",
            recommendedFix = "Check ABS module main fusible link (40A) and verify CAN communication line continuity."
        )
    )

    fun clearTroubleCodes() {
        _telemetry.value = _telemetry.value.copy(milCheckEngineOn = false)
    }

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
