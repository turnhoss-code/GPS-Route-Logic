package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.local.AppDatabase
import com.example.data.model.BillingPeriod
import com.example.data.model.ChatMessage
import com.example.data.model.ChatbotPersona
import com.example.data.model.DamageEvent
import com.example.data.model.DamageScoreReport
import com.example.data.model.DiagnosticScanRecord
import com.example.data.model.DtcCode
import com.example.data.model.FirestoreSyncState
import com.example.data.model.GeminiAiModel
import com.example.data.model.LiveTelemetry
import com.example.data.model.LiveVoiceSessionState
import com.example.data.model.MaintenanceTask
import com.example.data.model.RouteOption
import com.example.data.model.SavedRouteRecord
import com.example.data.model.SubscriptionTier
import com.example.data.model.TripLog
import com.example.data.model.UserAccount
import com.example.data.model.VehicleProfile
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.GeminiClient
import com.example.data.repository.DiagnosticsRepository
import com.example.data.repository.RouteNavigationRepository
import com.example.data.repository.VoiceAssistantRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    ROUTE("Route & Traffic"),
    DIAGNOSTICS("OBD-II AI Scan"),
    VOICE("Voice Co-Pilot"),
    GARAGE("My Garage"),
    ACCOUNT("Pricing & Account")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val authManager = AuthManager(application)
    val firestoreRepo = FirestoreRepository()
    val diagnosticsRepo = DiagnosticsRepository(
        db.scanDao(),
        db.vehicleDao(),
        db.maintenanceDao(),
        db.damageDao(),
        db.tripLogDao()
    )
    val routeRepo = RouteNavigationRepository(db.routeDao())
    val voiceRepo = VoiceAssistantRepository(application, authManager)

    // Current Tab
    private val _currentTab = MutableStateFlow(AppTab.ROUTE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Auth & Quota
    val userAccount: StateFlow<UserAccount> = authManager.currentUser
    val firestoreSyncState: StateFlow<FirestoreSyncState> = firestoreRepo.syncState

    private val _showPaywallModal = MutableStateFlow(false)
    val showPaywallModal: StateFlow<Boolean> = _showPaywallModal.asStateFlow()

    private val _selectedBillingPeriod = MutableStateFlow(BillingPeriod.MONTHLY)
    val selectedBillingPeriod: StateFlow<BillingPeriod> = _selectedBillingPeriod.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authStatusMessage = MutableStateFlow<String?>(null)
    val authStatusMessage: StateFlow<String?> = _authStatusMessage.asStateFlow()

    // Diagnostics State
    val telemetry: StateFlow<LiveTelemetry> = diagnosticsRepo.telemetry
    val isScanning: StateFlow<Boolean> = diagnosticsRepo.isScanning
    val scanHistory: StateFlow<List<DiagnosticScanRecord>> = diagnosticsRepo.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceTasks = diagnosticsRepo.maintenanceTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val damageEvents = diagnosticsRepo.damageEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tripLogs = diagnosticsRepo.tripLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val damageReport: StateFlow<DamageScoreReport> = diagnosticsRepo.damageReport

    private val _currentVehicleMileage = MutableStateFlow(45200)
    val currentVehicleMileage: StateFlow<Int> = _currentVehicleMileage.asStateFlow()

    private val _latestScan = MutableStateFlow<DiagnosticScanRecord?>(null)
    val latestScan: StateFlow<DiagnosticScanRecord?> = _latestScan.asStateFlow()

    private val _selectedDtc = MutableStateFlow<DtcCode?>(null)
    val selectedDtc: StateFlow<DtcCode?> = _selectedDtc.asStateFlow()

    private val _showFreezeFrameModal = MutableStateFlow(false)
    val showFreezeFrameModal: StateFlow<Boolean> = _showFreezeFrameModal.asStateFlow()

    // Route & Traffic State
    private val _originInput = MutableStateFlow("Downtown Financial District")
    val originInput: StateFlow<String> = _originInput.asStateFlow()

    private val _destinationInput = MutableStateFlow("Metro Tech Park, Bay Area")
    val destinationInput: StateFlow<String> = _destinationInput.asStateFlow()

    private val _routeOptions = MutableStateFlow<List<RouteOption>>(emptyList())
    val routeOptions: StateFlow<List<RouteOption>> = _routeOptions.asStateFlow()

    private val _selectedRoute = MutableStateFlow<RouteOption?>(null)
    val selectedRoute: StateFlow<RouteOption?> = _selectedRoute.asStateFlow()

    private val _aiTrafficAdvice = MutableStateFlow<String?>(null)
    val aiTrafficAdvice: StateFlow<String?> = _aiTrafficAdvice.asStateFlow()

    val isNavigating: StateFlow<Boolean> = routeRepo.isNavigating
    val currentNavStep: StateFlow<Int> = routeRepo.currentNavStep
    val activeNavRoute: StateFlow<RouteOption?> = routeRepo.activeRoute
    val liveRerouteAlert: StateFlow<String?> = routeRepo.liveRerouteSuggestion
    val savedRoutes: StateFlow<List<SavedRouteRecord>> = routeRepo.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Garage & Vehicles
    val vehicles: StateFlow<List<VehicleProfile>> = diagnosticsRepo.vehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI & Voice Assistant State
    val chatMessages: StateFlow<List<ChatMessage>> = voiceRepo.messages
    val selectedGeminiModel: StateFlow<GeminiAiModel> = voiceRepo.selectedModel
    val selectedPersona: StateFlow<ChatbotPersona> = voiceRepo.selectedPersona
    val selectedFemaleVoice: StateFlow<com.example.data.repository.GeminiFemaleVoice> = voiceRepo.selectedFemaleVoice
    val searchGroundingEnabled: StateFlow<Boolean> = voiceRepo.searchGroundingEnabled
    val mapsGroundingEnabled: StateFlow<Boolean> = voiceRepo.mapsGroundingEnabled
    val liveVoiceState: StateFlow<LiveVoiceSessionState> = voiceRepo.liveVoiceState
    val audioWaveformEnergy: StateFlow<Float> = voiceRepo.audioWaveformEnergy
    val liveTranscript: StateFlow<String> = voiceRepo.liveTranscript
    val isVoiceSpeaking: StateFlow<Boolean> = voiceRepo.isSpeaking
    val isChatProcessing: StateFlow<Boolean> = voiceRepo.isProcessing

    // Speech-To-Text (STT) States
    val isSttListening: StateFlow<Boolean> = voiceRepo.isSttListening
    val sttTranscript: StateFlow<String> = voiceRepo.sttTranscript
    val sttError: StateFlow<String?> = voiceRepo.sttError

    // Ad Simulation
    private val _showAdRewardDialog = MutableStateFlow(false)
    val showAdRewardDialog: StateFlow<Boolean> = _showAdRewardDialog.asStateFlow()

    private val _adWatchProgress = MutableStateFlow(0f)
    val adWatchProgress: StateFlow<Float> = _adWatchProgress.asStateFlow()

    init {
        calculateRoutes()

        // Wire live telemetry context provider into voice assistant repo
        voiceRepo.setTelemetryContextProvider {
            val telem = diagnosticsRepo.telemetry.value
            val vehicle = vehicles.value.firstOrNull { it.isDefault } ?: vehicles.value.firstOrNull()
            val damage = diagnosticsRepo.damageReport.value
            val activeCodes = diagnosticsRepo.sampleDtcDatabase.take(2)
            val dtcStr = activeCodes.joinToString { "${it.code}: ${it.description} (${it.severity.name})" }

            """
            Active Vehicle: ${vehicle?.year ?: 2024} ${vehicle?.make ?: "Honda"} ${vehicle?.model ?: "Accord"} ${vehicle?.trim ?: "Touring Elite"} (Engine: ${vehicle?.engine ?: "2.0L Turbo"}, Size: ${vehicle?.size ?: "Mid-Size Sedan"})
            Live Telemetry:
            - RPM: ${telem.rpm} RPM
            - Speed: ${telem.speedMph} MPH
            - Coolant Temp: ${telem.coolantTempF}°F (Optimal: 190-215°F)
            - Intake Air Temp: ${telem.intakeAirTempF}°F
            - Fuel Rail Pressure: ${telem.fuelRailPressurePsi} PSI
            - Throttle Position: ${telem.throttlePercent}%
            - Battery Voltage: ${telem.batteryVoltage}V
            - Mass Air Flow: ${telem.massAirFlowGps} g/s
            - Engine Load: ${telem.engineLoadPercent}%
            - Transmission Temp: ${telem.transmissionTempF}°F
            - Check Engine Light (MIL): ${if (telem.milCheckEngineOn) "ACTIVE" else "OFF"}
            Active DTCs: $dtcStr
            Damage Score: ${damage.overallScore}/100 (Powertrain: ${damage.powertrainScore}, Suspension: ${damage.suspensionScore}, Wear: ${damage.wearAssessment})
            """.trimIndent()
        }

        // Init vehicle profile if none exists
        viewModelScope.launch {
            db.vehicleDao().insertVehicle(
                VehicleProfile(
                    nickname = "Primary Commuter",
                    year = 2024,
                    make = "Honda",
                    model = "Accord",
                    trim = "Touring Elite Edition",
                    vin = "1HGCV1F34PA092811",
                    engine = "2.0L Turbo 4-Cyl DOHC 16V",
                    size = "Mid-Size Sedan (196.1″ L × 73.3″ W × 57.1″ H)",
                    bodyType = "4-Door Sedan / 5-Passenger",
                    transmission = "10-Speed Electronic Sport Automatic",
                    driveType = "Intelligent All-Wheel Drive (AWD)",
                    horsepower = "252 hp @ 6,500 RPM",
                    torque = "273 lb-ft @ 1,500–4,000 RPM",
                    fuelType = "Gasoline (Premium 91+ Unleaded)",
                    tankCapacity = "14.8 Gallons (~470 mi Range)",
                    curbWeight = "3,528 lbs (GVWR: 4,560 lbs)",
                    tireSpec = "235/40R19 96V (33 PSI Cold)",
                    towingCapacity = "1,500 lbs max",
                    obdProtocol = "ISO 15765-4 CAN (11-bit / 500 kbaud)",
                    ecuFirmware = "ECU-CAL-v4.8.2-HON",
                    oilSpec = "0W-20 Full Synthetic (4.4 qt)",
                    coolantSpec = "OEM Long Life Type 2 (6.8 qt)",
                    brakeFluidSpec = "DOT 4 Heavy Duty Synthetic",
                    mileage = 45200,
                    isDefault = true
                )
            )
            diagnosticsRepo.initDefaultMaintenanceIfEmpty()
        }
    }

    fun greetUserOnOpen(force: Boolean = false) {
        voiceRepo.greetUserOnOpen(force)
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setGeminiModel(model: GeminiAiModel) {
        voiceRepo.setModel(model)
    }

    fun setChatPersona(persona: ChatbotPersona) {
        voiceRepo.setPersona(persona)
    }

    fun setFemaleVoice(voice: com.example.data.repository.GeminiFemaleVoice) {
        voiceRepo.setGeminiFemaleVoice(voice)
    }

    fun playFemaleVoicePreview(voice: com.example.data.repository.GeminiFemaleVoice) {
        voiceRepo.setGeminiFemaleVoice(voice)
        voiceRepo.speak("Hi! I'm ${voice.displayName}. All vehicle sensors, ELM327 OBD diagnostics, and GPS systems are active.")
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        voiceRepo.toggleSearchGrounding(enabled)
    }

    fun toggleMapsGrounding(enabled: Boolean) {
        voiceRepo.toggleMapsGrounding(enabled)
    }

    fun startLiveVoiceSession() {
        voiceRepo.startLiveVoiceSession()
    }

    fun stopLiveVoiceSession() {
        voiceRepo.stopLiveVoiceSession()
    }

    fun startListeningLive() {
        voiceRepo.startListeningLive()
    }

    fun stopListeningAndSend(simulatedText: String? = null) {
        voiceRepo.stopListeningAndSend(simulatedText)
    }

    fun stopSpeaking() {
        voiceRepo.stopSpeaking()
    }

    fun clearChatHistory() {
        voiceRepo.clearHistory()
    }

    fun startSpeechToText(onResult: ((String) -> Unit)? = null) {
        voiceRepo.startSpeechRecognition(onResult)
    }

    fun stopSpeechToText() {
        voiceRepo.stopSpeechRecognition()
    }

    fun cancelSpeechToText() {
        voiceRepo.cancelSpeechRecognition()
    }

    fun executeVoiceCommand(command: String) {
        viewModelScope.launch {
            voiceRepo.sendQuery(command, isVoice = true)
        }
    }

    fun sendChatMessage(text: String, isVoice: Boolean = false) {
        viewModelScope.launch {
            voiceRepo.sendQuery(text, isVoice)
        }
    }

    fun syncWithFirestore() {
        viewModelScope.launch {
            val user = authManager.currentUser.value
            firestoreRepo.syncAllUserData(
                userId = user.uid,
                scans = scanHistory.value,
                vehicles = vehicles.value,
                routes = savedRoutes.value,
                messages = chatMessages.value
            )
        }
    }

    fun setOrigin(origin: String) {
        _originInput.value = origin
    }

    fun updateOrigin(origin: String) {
        _originInput.value = origin
    }

    fun setDestination(dest: String) {
        _destinationInput.value = dest
    }

    fun updateDestination(dest: String) {
        _destinationInput.value = dest
    }

    fun calculateRoutes() {
        viewModelScope.launch {
            val options = routeRepo.calculatePersonalizedRoutes(
                origin = _originInput.value,
                destination = _destinationInput.value
            )
            _routeOptions.value = options
            _selectedRoute.value = options.firstOrNull()

            // Generate AI Traffic advice
            val prompt = "Analyze traffic and route choices from ${_originInput.value} to ${_destinationInput.value}. Summarize incident delays and recommend the best departure window."
            val advice = GeminiClient.generateAiText(prompt)
            _aiTrafficAdvice.value = advice
        }
    }

    fun selectRoute(route: RouteOption) {
        _selectedRoute.value = route
    }

    fun startNavigation(route: RouteOption? = null) {
        val target = route ?: _selectedRoute.value ?: _routeOptions.value.firstOrNull() ?: return
        routeRepo.startNavigation(target)
    }

    fun startNavigating(route: RouteOption? = null) {
        startNavigation(route)
    }

    fun stopNavigation() {
        routeRepo.stopNavigation()
    }

    fun stopNavigating() {
        routeRepo.stopNavigation()
    }

    fun advanceNavStep() {
        routeRepo.nextStep()
    }

    fun nextNavStep() {
        routeRepo.nextStep()
    }

    fun acceptReroute() {
        routeRepo.acceptReroute()
    }

    fun dismissReroute() {
        routeRepo.dismissReroute()
    }

    fun saveCurrentRoute() {
        val route = _selectedRoute.value ?: return
        viewModelScope.launch {
            routeRepo.saveRoute(_originInput.value, _destinationInput.value, route)
            syncWithFirestore()
        }
    }

    fun triggerAiScan(deepScan: Boolean = false) {
        viewModelScope.launch {
            val hasQuota = authManager.recordScanUsage()
            if (!hasQuota) {
                _showPaywallModal.value = true
                return@launch
            }

            val defaultCar = vehicles.value.find { it.isDefault } ?: vehicles.value.firstOrNull()
            val scan = diagnosticsRepo.performAiDiagnosticScan(
                vehicleName = defaultCar?.let { "${it.year} ${it.make} ${it.model}" } ?: "2024 Honda Accord Hybrid",
                vin = defaultCar?.vin ?: "1HGCV1F34PA092811",
                deepScan = deepScan
            )
            _latestScan.value = scan
            syncWithFirestore()
        }
    }

    fun runObdScan() = triggerAiScan(false)

    fun parseDtcCodes(json: String): List<DtcCode> {
        return diagnosticsRepo.parseDtcCodes(json)
    }

    fun selectDtc(code: DtcCode?) {
        _selectedDtc.value = code
    }

    fun clearEcuCodes() {
        viewModelScope.launch {
            diagnosticsRepo.clearCodes()
        }
    }

    fun toggleFreezeFrameModal(show: Boolean) {
        _showFreezeFrameModal.value = show
    }

    fun triggerGoogleSignIn(webClientId: String? = null) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _authStatusMessage.value = "Connecting to Google Account..."
            val result = authManager.signInWithGoogle(webClientId)
            _isSigningIn.value = false
            if (result.isSuccess) {
                _authStatusMessage.value = "Welcome back, ${result.getOrNull()?.displayName}!"
                syncWithFirestore()
            } else {
                _authStatusMessage.value = result.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        }
    }

    fun signInWithGoogle(webClientId: String? = null) {
        triggerGoogleSignIn(webClientId)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _authStatusMessage.value = "Signed out of Google Account"
        }
    }

    fun showPaywall(show: Boolean = true) {
        _showPaywallModal.value = show
    }

    fun openPaywall(billingPeriod: BillingPeriod = BillingPeriod.MONTHLY) {
        _selectedBillingPeriod.value = billingPeriod
        _showPaywallModal.value = true
    }

    fun closePaywall() {
        _showPaywallModal.value = false
    }

    fun setBillingPeriod(period: BillingPeriod) {
        _selectedBillingPeriod.value = period
    }

    fun upgradeSubscription(tier: SubscriptionTier) {
        authManager.updateSubscriptionTier(tier, _selectedBillingPeriod.value)
        _showPaywallModal.value = false
    }

    fun upgradeTier(tier: SubscriptionTier) {
        upgradeSubscription(tier)
    }

    fun showAdReward() {
        _showAdRewardDialog.value = true
        _adWatchProgress.value = 0f
        viewModelScope.launch {
            for (i in 1..10) {
                delay(300)
                _adWatchProgress.value = i / 10f
            }
            authManager.grantBonusAdScan()
            delay(400)
            _showAdRewardDialog.value = false
        }
    }

    fun dismissAdReward() {
        _showAdRewardDialog.value = false
    }

    fun addVehicle(
        nickname: String,
        year: Int,
        make: String,
        model: String,
        trim: String = "Sport Touring Edition",
        engine: String = "2.0L Turbo 4-Cyl DOHC 16V",
        size: String = "Mid-Size Sedan (196.1″ L × 73.3″ W × 57.1″ H)",
        transmission: String = "10-Speed Electronic Sport Automatic",
        driveType: String = "Intelligent All-Wheel Drive (AWD)",
        horsepower: String = "252 hp @ 6,500 RPM",
        fuelType: String = "Gasoline (Premium 91+ Unleaded)",
        tireSpec: String = "235/40R19 96V (33 PSI Cold)",
        mileage: Int = 45200,
        vin: String = ""
    ) {
        viewModelScope.launch {
            diagnosticsRepo.addVehicle(
                VehicleProfile(
                    nickname = nickname,
                    year = year,
                    make = make,
                    model = model,
                    trim = trim,
                    engine = engine,
                    size = size,
                    transmission = transmission,
                    driveType = driveType,
                    horsepower = horsepower,
                    fuelType = fuelType,
                    tireSpec = tireSpec,
                    mileage = mileage,
                    vin = vin,
                    isDefault = false
                )
            )
            syncWithFirestore()
        }
    }

    fun updateVehicle(vehicle: VehicleProfile) {
        viewModelScope.launch {
            diagnosticsRepo.updateVehicle(vehicle)
            syncWithFirestore()
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            diagnosticsRepo.deleteVehicle(id)
            syncWithFirestore()
        }
    }

    fun setDefaultVehicle(id: Long) {
        viewModelScope.launch {
            diagnosticsRepo.setDefaultVehicle(id)
            syncWithFirestore()
        }
    }

    fun deleteAllUserDataAndReset(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val user = authManager.currentUser.value
            // 1. Delete from Firestore
            firestoreRepo.deleteAllUserData(user.uid)

            // 2. Clear database tables
            db.scanDao().clearAllScans()
            db.vehicleDao().clearAllVehicles()
            db.routeDao().clearAllRoutes()

            // 3. Sign out and reset quotas
            authManager.signOut()

            // 4. Reset state
            _latestScan.value = null
            _selectedDtc.value = null
            _authStatusMessage.value = "All account data, routes, diagnostics & Firestore records permanently deleted"

            // Re-seed default demo car for fresh start
            db.vehicleDao().insertVehicle(
                VehicleProfile(
                    nickname = "Primary Commuter",
                    year = 2024,
                    make = "Toyota",
                    model = "RAV4 Hybrid",
                    vin = "4T3DFREV9RU129481",
                    isDefault = true
                )
            )

            onComplete()
        }
    }

    fun markMaintenanceCompleted(id: String) {
        viewModelScope.launch {
            diagnosticsRepo.markMaintenanceTaskCompleted(id, _currentVehicleMileage.value)
            syncWithFirestore()
        }
    }

    fun addMaintenanceTask(name: String, type: String, intervalMiles: Int, intervalMonths: Int) {
        viewModelScope.launch {
            val task = com.example.data.model.MaintenanceTask(
                id = System.currentTimeMillis().toString(),
                name = name,
                type = type,
                intervalMiles = intervalMiles,
                intervalMonths = intervalMonths,
                lastCompletedMiles = _currentVehicleMileage.value,
                lastCompletedDate = System.currentTimeMillis()
            )
            diagnosticsRepo.insertMaintenanceTask(task)
            syncWithFirestore()
        }
    }

    fun deleteScanRecord(id: Long) {
        viewModelScope.launch {
            diagnosticsRepo.removeScan(id)
            if (_latestScan.value?.id == id) {
                _latestScan.value = null
            }
            syncWithFirestore()
        }
    }

    fun simulateTelemetryThrottle(deltaRpm: Int, deltaSpeed: Int) {
        diagnosticsRepo.updateTelemetrySimulated(deltaRpm, deltaSpeed)
    }

    fun inspectScanRecord(record: DiagnosticScanRecord) {
        _latestScan.value = record
    }

    fun updateVehicleMileage(newMileage: Int) {
        _currentVehicleMileage.value = newMileage
    }

    override fun onCleared() {
        super.onCleared()
        voiceRepo.cleanup()
    }
}
