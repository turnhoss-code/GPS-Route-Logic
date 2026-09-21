package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.RouteOption
import com.example.data.model.RouteSuggestionType
import com.example.data.model.TrafficIncident
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.routeDataStore: DataStore<Preferences> by preferencesDataStore(name = "cached_gps_routes")

/**
 * DataStoreManager facilitates local caching of frequently used GPS routes
 * to ensure uninterrupted navigation co-pilot functionality when internet connectivity is lost.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        private const val TAG = "DataStoreManager"
        val KEY_CACHED_ROUTES_JSON = stringPreferencesKey("cached_gps_routes_json")
        val KEY_ACTIVE_NAV_ROUTE_ID = stringPreferencesKey("active_nav_route_id")
        val KEY_LAST_CACHED_TIMESTAMP = stringPreferencesKey("last_cached_timestamp")

        val DEFAULT_OFFLINE_ROUTES = listOf(
            RouteOption(
                id = "route_fastest_offline",
                name = "I-80 W Express Bypass (Offline Cached)",
                type = RouteSuggestionType.FASTEST,
                distanceMiles = 14.8,
                durationMinutes = 24,
                trafficDelayMinutes = 0,
                trafficLevel = "Clear (Offline Preset)",
                tollCost = 2.50,
                fuelSavingsPercent = 6,
                co2SavedKg = 0.5,
                summary = "Primary offline artery with cached landmark waypoints and zero network latency.",
                personalizedReason = "Cached for instant turn-by-turn guidance without cellular reception.",
                waypoints = listOf("Current GPS Location", "Grand Avenue", "I-80 Express Tollway", "Exit 44B", "Silicon Corridor Destination"),
                incidents = emptyList()
            ),
            RouteOption(
                id = "route_eco_offline",
                name = "Eco-Logic Low-Wear Corridor (Offline Cached)",
                type = RouteSuggestionType.ECO_FRIENDLY,
                distanceMiles = 13.9,
                durationMinutes = 28,
                trafficDelayMinutes = 0,
                trafficLevel = "Clear",
                tollCost = 0.00,
                fuelSavingsPercent = 16,
                co2SavedKg = 1.9,
                summary = "Smooth asphalt route avoiding rough pavement and high vibration zones.",
                personalizedReason = "Preserves suspension damping and avoids pothole clusters without internet.",
                waypoints = listOf("Current GPS Location", "Grand Avenue Parkway", "Boulevard 101", "Silicon Corridor Destination"),
                incidents = emptyList()
            ),
            RouteOption(
                id = "route_scenic_offline",
                name = "Skyline Safe Coastal Bypass (Offline Cached)",
                type = RouteSuggestionType.SCENIC_SMOOTH,
                distanceMiles = 17.2,
                durationMinutes = 32,
                trafficDelayMinutes = 0,
                trafficLevel = "Clear",
                tollCost = 0.00,
                fuelSavingsPercent = 7,
                co2SavedKg = 0.8,
                summary = "Panoramic mountain ridgeline with fresh asphalt and clear visual landmarks.",
                personalizedReason = "Offline fallback route with wide shoulders and emergency pullouts.",
                waypoints = listOf("Current GPS Location", "Skyline Overlook Rd", "Valley Ridge", "Silicon Corridor Destination"),
                incidents = emptyList()
            ),
            RouteOption(
                id = "route_heavy_offline",
                name = "Commercial Heavy Clearance Safe (Offline Cached)",
                type = RouteSuggestionType.HEAVY_VEHICLE,
                distanceMiles = 16.4,
                durationMinutes = 30,
                trafficDelayMinutes = 0,
                trafficLevel = "Normal",
                tollCost = 4.00,
                fuelSavingsPercent = 2,
                co2SavedKg = 0.2,
                summary = "Guaranteed 14'6\" bridge clearance with low-gradient arterial roadways.",
                personalizedReason = "Prevents low-clearance hazards during off-grid operations.",
                waypoints = listOf("Current GPS Location", "Industrial Bypass Ring", "Freight Hub Connector", "Silicon Corridor Destination"),
                incidents = emptyList()
            )
        )
    }

    /**
     * Flow of cached routes for offline navigation access.
     * Emits default offline routes if DataStore is empty.
     */
    val cachedRoutesFlow: Flow<List<RouteOption>> = context.routeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading route DataStore preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonString = preferences[KEY_CACHED_ROUTES_JSON]
            if (jsonString.isNullOrBlank()) {
                DEFAULT_OFFLINE_ROUTES
            } else {
                parseRoutesFromJson(jsonString).ifEmpty { DEFAULT_OFFLINE_ROUTES }
            }
        }

    val activeNavRouteIdFlow: Flow<String?> = context.routeDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[KEY_ACTIVE_NAV_ROUTE_ID] }

    /**
     * Persists a list of GPS routes into DataStore preferences for offline navigation.
     */
    suspend fun saveCachedRoutes(routes: List<RouteOption>) {
        try {
            val jsonArray = JSONArray()
            routes.forEach { route ->
                jsonArray.put(serializeRoute(route))
            }
            context.routeDataStore.edit { preferences ->
                preferences[KEY_CACHED_ROUTES_JSON] = jsonArray.toString()
                preferences[KEY_LAST_CACHED_TIMESTAMP] = System.currentTimeMillis().toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving routes to DataStore", e)
        }
    }

    /**
     * Records or updates a single route into DataStore, ensuring it's available offline.
     */
    suspend fun cacheRoute(route: RouteOption) {
        try {
            context.routeDataStore.edit { preferences ->
                val existingJson = preferences[KEY_CACHED_ROUTES_JSON]
                val currentList = if (!existingJson.isNullOrBlank()) {
                    parseRoutesFromJson(existingJson).toMutableList()
                } else {
                    DEFAULT_OFFLINE_ROUTES.toMutableList()
                }

                val existingIndex = currentList.indexOfFirst { it.id == route.id }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = route
                } else {
                    currentList.add(0, route)
                }

                // Keep maximum 20 cached offline routes
                val trimmedList = currentList.take(20)
                val jsonArray = JSONArray()
                trimmedList.forEach { jsonArray.put(serializeRoute(it)) }

                preferences[KEY_CACHED_ROUTES_JSON] = jsonArray.toString()
                preferences[KEY_ACTIVE_NAV_ROUTE_ID] = route.id
                preferences[KEY_LAST_CACHED_TIMESTAMP] = System.currentTimeMillis().toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed caching route to DataStore", e)
        }
    }

    /**
     * Sets the ID of the actively navigated route for resume capability across crashes/disconnects.
     */
    suspend fun setActiveRouteId(routeId: String?) {
        try {
            context.routeDataStore.edit { preferences ->
                if (routeId != null) {
                    preferences[KEY_ACTIVE_NAV_ROUTE_ID] = routeId
                } else {
                    preferences.remove(KEY_ACTIVE_NAV_ROUTE_ID)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting active route ID in DataStore", e)
        }
    }

    /**
     * Clears cached routes from DataStore and resets to default offline routes.
     */
    suspend fun clearCachedRoutes() {
        try {
            context.routeDataStore.edit { preferences ->
                preferences.remove(KEY_CACHED_ROUTES_JSON)
                preferences.remove(KEY_ACTIVE_NAV_ROUTE_ID)
                preferences.remove(KEY_LAST_CACHED_TIMESTAMP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing route DataStore", e)
        }
    }

    private fun serializeRoute(route: RouteOption): JSONObject {
        val json = JSONObject()
        json.put("id", route.id)
        json.put("name", route.name)
        json.put("type", route.type.name)
        json.put("distanceMiles", route.distanceMiles)
        json.put("durationMinutes", route.durationMinutes)
        json.put("trafficDelayMinutes", route.trafficDelayMinutes)
        json.put("trafficLevel", route.trafficLevel)
        json.put("tollCost", route.tollCost)
        json.put("fuelSavingsPercent", route.fuelSavingsPercent)
        json.put("co2SavedKg", route.co2SavedKg)
        json.put("summary", route.summary)
        json.put("personalizedReason", route.personalizedReason)

        val waypointsArray = JSONArray()
        route.waypoints.forEach { waypointsArray.put(it) }
        json.put("waypoints", waypointsArray)

        val incidentsArray = JSONArray()
        route.incidents.forEach { incident ->
            val incObj = JSONObject()
            incObj.put("id", incident.id)
            incObj.put("type", incident.type)
            incObj.put("title", incident.title)
            incObj.put("description", incident.description)
            incObj.put("location", incident.location)
            incObj.put("delayMinutes", incident.delayMinutes)
            incObj.put("severity", incident.severity)
            incidentsArray.put(incObj)
        }
        json.put("incidents", incidentsArray)

        return json
    }

    private fun parseRoutesFromJson(jsonStr: String): List<RouteOption> {
        val list = mutableListOf<RouteOption>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeName = obj.optString("type", RouteSuggestionType.FASTEST.name)
                val routeType = try {
                    RouteSuggestionType.valueOf(typeName)
                } catch (_: Throwable) {
                    RouteSuggestionType.FASTEST
                }

                val waypointsList = mutableListOf<String>()
                val wpArray = obj.optJSONArray("waypoints")
                if (wpArray != null) {
                    for (w in 0 until wpArray.length()) {
                        waypointsList.add(wpArray.getString(w))
                    }
                }

                val incidentsList = mutableListOf<TrafficIncident>()
                val incArray = obj.optJSONArray("incidents")
                if (incArray != null) {
                    for (incIdx in 0 until incArray.length()) {
                        val incObj = incArray.getJSONObject(incIdx)
                        incidentsList.add(
                            TrafficIncident(
                                id = incObj.optString("id", "inc_$incIdx"),
                                type = incObj.optString("type", "Notice"),
                                title = incObj.optString("title", "Traffic Notice"),
                                description = incObj.optString("description", ""),
                                location = incObj.optString("location", "Route Segment"),
                                delayMinutes = incObj.optInt("delayMinutes", 0),
                                severity = incObj.optString("severity", "Info")
                            )
                        )
                    }
                }

                list.add(
                    RouteOption(
                        id = obj.optString("id", "cached_route_$i"),
                        name = obj.optString("name", "Cached Route $i"),
                        type = routeType,
                        distanceMiles = obj.optDouble("distanceMiles", 10.0),
                        durationMinutes = obj.optInt("durationMinutes", 15),
                        trafficDelayMinutes = obj.optInt("trafficDelayMinutes", 0),
                        trafficLevel = obj.optString("trafficLevel", "Normal"),
                        tollCost = obj.optDouble("tollCost", 0.0),
                        fuelSavingsPercent = obj.optInt("fuelSavingsPercent", 0),
                        co2SavedKg = obj.optDouble("co2SavedKg", 0.0),
                        summary = obj.optString("summary", "Offline cached route option"),
                        personalizedReason = obj.optString("personalizedReason", "Offline availability"),
                        waypoints = waypointsList,
                        incidents = incidentsList
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing routes from DataStore JSON", e)
        }
        return list
    }
}
