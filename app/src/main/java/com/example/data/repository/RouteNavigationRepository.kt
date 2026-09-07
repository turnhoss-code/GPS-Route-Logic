package com.example.data.repository

import com.example.data.local.RouteDao
import com.example.data.model.RouteOption
import com.example.data.model.RouteSuggestionType
import com.example.data.model.SavedRouteRecord
import com.example.data.model.TrafficIncident
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class RouteNavigationRepository(
    private val routeDao: RouteDao
) {
    val savedRoutes: Flow<List<SavedRouteRecord>> = routeDao.getAllSavedRoutes()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _currentNavStep = MutableStateFlow(0)
    val currentNavStep: StateFlow<Int> = _currentNavStep.asStateFlow()

    private val _activeRoute = MutableStateFlow<RouteOption?>(null)
    val activeRoute: StateFlow<RouteOption?> = _activeRoute.asStateFlow()

    private val _liveRerouteSuggestion = MutableStateFlow<String?>(null)
    val liveRerouteSuggestion: StateFlow<String?> = _liveRerouteSuggestion.asStateFlow()

    val turnByTurnSteps = listOf(
        "Head North on Grand Avenue toward Pine St (0.4 mi)",
        "In 500 ft, merge onto I-80 West via the right ramp (3.2 mi)",
        "Stay in the 2 left lanes to take Exit 44B for Downtown Express (1.8 mi)",
        "Traffic slowdown ahead: Maintain 45 MPH in middle lane (0.9 mi)",
        "Take slight right onto Lakeview Boulevard (0.6 mi)",
        "Turn right into destination plaza on your right"
    )

    fun calculatePersonalizedRoutes(origin: String, destination: String): List<RouteOption> {
        val originLabel = origin.trim().ifBlank { "Current Location" }
        val destLabel = destination.trim().ifBlank { "Selected Destination" }
        val baseDistance = 14.8
        val baseMinutes = 26

        val fastest = RouteOption(
            id = "route_fastest_01",
            name = "Fastest Route via Express Highway",
            type = RouteSuggestionType.FASTEST,
            distanceMiles = baseDistance,
            durationMinutes = baseMinutes,
            trafficDelayMinutes = 3,
            trafficLevel = "Moderate",
            tollCost = 2.50,
            fuelSavingsPercent = 4,
            co2SavedKg = 0.4,
            summary = "Direct highway connection with active flow bypass sensors.",
            personalizedReason = "Best for quick arrival during evening rush hour.",
            waypoints = listOf(originLabel, "I-80 Express Tollway", "Exit 44B", destLabel),
            incidents = listOf(
                TrafficIncident(
                    id = "inc_01",
                    type = "Congestion",
                    title = "Moderate Slowdown on I-80 W",
                    description = "Average speed 38 MPH near junction 12 due to lane merge.",
                    location = "Mile Marker 14.2",
                    delayMinutes = 3,
                    severity = "Warning"
                )
            )
        )

        val eco = RouteOption(
            id = "route_eco_02",
            name = "Eco-Logic Green Corridor",
            type = RouteSuggestionType.ECO_FRIENDLY,
            distanceMiles = 13.9,
            durationMinutes = 29,
            trafficDelayMinutes = 1,
            trafficLevel = "Light",
            tollCost = 0.00,
            fuelSavingsPercent = 16,
            co2SavedKg = 1.9,
            summary = "Synchronized traffic lights & constant speed cruising to minimize braking.",
            personalizedReason = "Saves ~0.35 gal fuel ($1.45) & reduces brake pad wear.",
            waypoints = listOf(originLabel, "Grand Avenue Parkway", "Boulevard 101", destLabel),
            incidents = emptyList()
        )

        val scenic = RouteOption(
            id = "route_scenic_03",
            name = "Scenic Coastal & Ridge View",
            type = RouteSuggestionType.SCENIC_SMOOTH,
            distanceMiles = 17.2,
            durationMinutes = 34,
            trafficDelayMinutes = 0,
            trafficLevel = "Clear",
            tollCost = 0.00,
            fuelSavingsPercent = 7,
            co2SavedKg = 0.8,
            summary = "Sweeping curves, scenic vistas, and ultra-smooth asphalt quality.",
            personalizedReason = "Ideal for relaxed driving with zero highway stress.",
            waypoints = listOf(originLabel, "Skyline Overlook Rd", "Valley Ridge", destLabel),
            incidents = listOf(
                TrafficIncident(
                    id = "inc_02",
                    type = "Notice",
                    title = "Scenic Viewpoint Ahead",
                    description = "Panoramic sunset overlook pullout in 2.5 miles.",
                    location = "Skyline Summit",
                    delayMinutes = 0,
                    severity = "Info"
                )
            )
        )

        val heavy = RouteOption(
            id = "route_heavy_04",
            name = "Commercial Cargo & Truck Safe",
            type = RouteSuggestionType.HEAVY_VEHICLE,
            distanceMiles = 16.4,
            durationMinutes = 31,
            trafficDelayMinutes = 2,
            trafficLevel = "Moderate",
            tollCost = 4.00,
            fuelSavingsPercent = 2,
            co2SavedKg = 0.2,
            summary = "Guaranteed 14'6\" bridge clearance, wide turning radius, certified axle weight.",
            personalizedReason = "Prevents low-clearance hazards & steep 8%+ gradients.",
            waypoints = listOf(originLabel, "Industrial Bypass Ring", "Freight Hub Connector", destLabel),
            incidents = emptyList()
        )

        return listOf(fastest, eco, scenic, heavy)
    }

    suspend fun getAiTrafficAdvice(origin: String, destination: String, selectedRoute: RouteOption): String {
        val prompt = "User is navigating from '$origin' to '$destination' using ${selectedRoute.name} (${selectedRoute.type.label}). Distance: ${selectedRoute.distanceMiles} miles, ETA: ${selectedRoute.durationMinutes} mins, Traffic: ${selectedRoute.trafficLevel}. Give 1 personalized real-time traffic tip and fuel optimization advice."
        return GeminiClient.generateAiText(
            prompt = prompt,
            systemInstruction = "You are GPS Route Logic's navigation AI. Respond in 2 crisp, actionable sentences with lane/traffic advice."
        )
    }

    fun startNavigation(route: RouteOption) {
        _activeRoute.value = route
        _isNavigating.value = true
        _currentNavStep.value = 0
        _liveRerouteSuggestion.value = null
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _activeRoute.value = null
        _liveRerouteSuggestion.value = null
    }

    fun nextStep() {
        val curr = _currentNavStep.value
        if (curr < turnByTurnSteps.size - 1) {
            _currentNavStep.value = curr + 1
            if (curr == 1) {
                _liveRerouteSuggestion.value = "Traffic bottleneck detected 1.2 mi ahead (+6 min delay). Faster bypass available via Ridge Exit 12 (Save 5 mins)!"
            }
        } else {
            stopNavigation()
        }
    }

    fun acceptReroute() {
        _liveRerouteSuggestion.value = null
        val currentRoute = _activeRoute.value
        if (currentRoute != null) {
            _activeRoute.value = currentRoute.copy(
                name = "${currentRoute.name} (Bypass Applied)",
                durationMinutes = (currentRoute.durationMinutes - 5).coerceAtLeast(15),
                trafficDelayMinutes = 0,
                trafficLevel = "Clear"
            )
        }
    }

    fun dismissReroute() {
        _liveRerouteSuggestion.value = null
    }

    suspend fun saveRoute(origin: String, destination: String, route: RouteOption) {
        routeDao.insertRoute(
            SavedRouteRecord(
                origin = origin,
                destination = destination,
                routeTypeName = route.type.label,
                distanceMiles = route.distanceMiles,
                durationMinutes = route.durationMinutes,
                fuelSaved = "${route.fuelSavingsPercent}%"
            )
        )
    }
}
