package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveTelemetry
import com.example.data.model.RouteOption
import com.example.data.model.RouteSuggestionType
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen

enum class MapThemeMode(val displayName: String, val badge: String) {
    SATELLITE_HYBRID("Satellite Hybrid", "🛰️ SATELLITE / HYBRID"),
    ROADMAP("Roadmap Standard", "🗺️ ROADMAP"),
    CYBER_HUD("Cyber HUD", "⚡ CAN-BUS HUD")
}

@Composable
fun MapVisualizer(
    modifier: Modifier = Modifier,
    selectedRoute: RouteOption?,
    alternateRoutes: List<RouteOption> = emptyList(),
    isNavigating: Boolean = false,
    navStep: Int = 0,
    currentLocationName: String = "450 Mission St, San Francisco, CA",
    currentGpsCoords: String = "37.7749° N, 122.4194° W",
    telemetry: LiveTelemetry = LiveTelemetry(),
    weatherCondition: String = "Wet Asphalt • Rain 68°F",
    damageScore: Int = 94,
    onSelectAlternateRoute: ((RouteOption) -> Unit)? = null
) {
    var showDamageLayer by remember { mutableStateOf(true) }
    var showWeatherLayer by remember { mutableStateOf(true) }
    var showTelemetryHud by remember { mutableStateOf(true) }
    var showTrafficLayer by remember { mutableStateOf(true) }
    // Default map mode is Satellite/Roadmap Hybrid
    var mapTheme by remember { mutableStateOf(MapThemeMode.SATELLITE_HYBRID) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Map Pan and Zoom State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "map_anim")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                when (mapTheme) {
                    MapThemeMode.SATELLITE_HYBRID -> Color(0xFF071524) // Deep satellite earth/ocean base
                    MapThemeMode.ROADMAP -> Color(0xFF0F172A)          // Dark modern vector roadmap
                    MapThemeMode.CYBER_HUD -> Color(0xFF0B132B)        // High-tech HUD grid
                }
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
                    panOffsetX = (panOffsetX + pan.x).coerceIn(-250f, 250f)
                    panOffsetY = (panOffsetY + pan.y).coerceIn(-250f, 250f)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gps_map_canvas")
        ) {
            val width = size.width
            val height = size.height

            // ==========================================
            // 1. SATELLITE BASE TERRAIN & WATERWAYS
            // ==========================================
            if (mapTheme == MapThemeMode.SATELLITE_HYBRID) {
                // (a) Base Coastal & Ocean Water (SF Bay & Pacific Harbor)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF081C30), Color(0xFF0B2540), Color(0xFF061424))
                    )
                )

                // (b) Satellite Landmass Polygonal Contours (Urban Peninsula & Coastline)
                val landmassPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width * 0.72f + panOffsetX, 0f)
                    cubicTo(
                        width * 0.68f + panOffsetX, height * 0.30f + panOffsetY,
                        width * 0.85f + panOffsetX, height * 0.45f + panOffsetY,
                        width * 0.60f + panOffsetX, height * 0.70f + panOffsetY
                    )
                    cubicTo(
                        width * 0.45f + panOffsetX, height * 0.88f + panOffsetY,
                        width * 0.20f + panOffsetX, height * 0.95f + panOffsetY,
                        0f, height
                    )
                    close()
                }

                // Satellite Earth / Terrain Texture
                drawPath(
                    path = landmassPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E2F2B), Color(0xFF172421), Color(0xFF101917)),
                        center = Offset(width * 0.35f + panOffsetX, height * 0.45f + panOffsetY),
                        radius = width * 0.8f
                    )
                )

                // (c) Forest & Green Space Satellite Patches (Presidio / Golden Gate Park / Hills)
                val parkPath1 = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = width * 0.05f + panOffsetX,
                            top = height * 0.22f + panOffsetY,
                            right = width * 0.32f + panOffsetX,
                            bottom = height * 0.34f + panOffsetY,
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    )
                }
                drawPath(path = parkPath1, color = Color(0xFF1B3D2F).copy(alpha = 0.85f))

                val parkPath2 = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = width * 0.12f + panOffsetX,
                            top = height * 0.62f + panOffsetY,
                            right = width * 0.38f + panOffsetX,
                            bottom = height * 0.76f + panOffsetY,
                            cornerRadius = CornerRadius(16f, 16f)
                        )
                    )
                }
                drawPath(path = parkPath2, color = Color(0xFF204232).copy(alpha = 0.75f))

                // (d) Satellite Urban Footprint Density Grid (Downtown / SOMA Rooftops)
                val blockColor = Color(0xFF2C3948).copy(alpha = 0.35f)
                val blockBorder = Color(0xFF384659).copy(alpha = 0.5f)
                for (r in 0..4) {
                    for (c in 0..5) {
                        val bx = (width * 0.28f + (c * 32f * zoomScale) + panOffsetX)
                        val by = (height * 0.35f + (r * 24f * zoomScale) + panOffsetY)
                        if (bx in 0f..width && by in 0f..height) {
                            drawRoundRect(
                                color = blockColor,
                                topLeft = Offset(bx, by),
                                size = Size(24f * zoomScale, 18f * zoomScale),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                            drawRoundRect(
                                color = blockBorder,
                                topLeft = Offset(bx, by),
                                size = Size(24f * zoomScale, 18f * zoomScale),
                                cornerRadius = CornerRadius(3f, 3f),
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }

                // Coastline Shoreline Ripple Glow
                drawPath(
                    path = landmassPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    style = Stroke(width = 2.5f * zoomScale)
                )

                // (e) Bridge Spans over Bay (Bay Bridge & Golden Gate Spans)
                // Bay Bridge East Span
                val bridgePath = Path().apply {
                    moveTo(width * 0.60f + panOffsetX, height * 0.42f + panOffsetY)
                    lineTo(width * 0.98f + panOffsetX, height * 0.36f + panOffsetY)
                }
                drawPath(
                    path = bridgePath,
                    color = Color(0xFF1E293B),
                    style = Stroke(width = 10f * zoomScale, cap = StrokeCap.Square)
                )
                drawPath(
                    path = bridgePath,
                    color = Color(0xFF94A3B8),
                    style = Stroke(width = 4f * zoomScale, cap = StrokeCap.Square)
                )
                // Bridge Pier Towers
                drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(width * 0.72f + panOffsetX, height * 0.40f + panOffsetY))
                drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(width * 0.85f + panOffsetX, height * 0.38f + panOffsetY))
            } else {
                // Roadmap / HUD Topo Grid
                val gridColor = if (mapTheme == MapThemeMode.ROADMAP) Color(0xFF1E293B) else Color(0xFF1C2A4A)
                val gridSpacing = 40.dp.toPx()
                var gx = (panOffsetX % gridSpacing)
                while (gx < width) {
                    if (gx >= 0) {
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, height),
                            strokeWidth = 1f
                        )
                    }
                    gx += gridSpacing
                }
                var gy = (panOffsetY % gridSpacing)
                while (gy < height) {
                    if (gy >= 0) {
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(0f, gy),
                            end = Offset(width, gy),
                            strokeWidth = 1f
                        )
                    }
                    gy += gridSpacing
                }
            }

            // ==========================================
            // 2. HYBRID ROADMAP VECTOR OVERLAYS
            // ==========================================
            // Secondary City Street Grid
            val secondaryStreetColor = if (mapTheme == MapThemeMode.SATELLITE_HYBRID) Color(0x66CBD5E1) else Color(0x3364748B)
            for (i in 1..4) {
                val sy = height * (0.20f + (i * 0.14f)) + panOffsetY
                drawLine(
                    color = secondaryStreetColor,
                    start = Offset(width * 0.10f + panOffsetX, sy),
                    end = Offset(width * 0.65f + panOffsetX, sy),
                    strokeWidth = 2.5f * zoomScale
                )
            }
            for (i in 1..4) {
                val sx = width * (0.15f + (i * 0.12f)) + panOffsetX
                drawLine(
                    color = secondaryStreetColor,
                    start = Offset(sx, height * 0.18f + panOffsetY),
                    end = Offset(sx, height * 0.82f + panOffsetY),
                    strokeWidth = 2.5f * zoomScale
                )
            }

            // Major Arterial Highway: Interstate 80 (Bay Bridge Corridor)
            val hwCasingColor = Color(0xFF0F172A)
            val hwSurfaceColor = if (mapTheme == MapThemeMode.SATELLITE_HYBRID) Color(0xFFFFB703) else Color(0xFFF59E0B)

            // I-80 Freeway Casing & Surface
            drawLine(
                color = hwCasingColor,
                start = Offset(0f + panOffsetX, height * 0.38f + panOffsetY),
                end = Offset(width + panOffsetX, height * 0.44f + panOffsetY),
                strokeWidth = 18f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = hwSurfaceColor,
                start = Offset(0f + panOffsetX, height * 0.38f + panOffsetY),
                end = Offset(width + panOffsetX, height * 0.44f + panOffsetY),
                strokeWidth = 11f * zoomScale,
                cap = StrokeCap.Round
            )

            // US-101 Highway Diagonal Corridor
            val us101Color = if (mapTheme == MapThemeMode.SATELLITE_HYBRID) Color(0xFFFB8500) else Color(0xFFEA580C)
            drawLine(
                color = hwCasingColor,
                start = Offset(width * 0.10f + panOffsetX, height * 0.85f + panOffsetY),
                end = Offset(width * 0.90f + panOffsetX, height * 0.20f + panOffsetY),
                strokeWidth = 16f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = us101Color,
                start = Offset(width * 0.10f + panOffsetX, height * 0.85f + panOffsetY),
                end = Offset(width * 0.90f + panOffsetX, height * 0.20f + panOffsetY),
                strokeWidth = 9f * zoomScale,
                cap = StrokeCap.Round
            )

            // North-South Arterial: Van Ness & Embarcadero Boulevards
            val arterialColor = if (mapTheme == MapThemeMode.SATELLITE_HYBRID) Color(0xFFF8FAFC) else Color(0xFFE2E8F0)
            drawLine(
                color = hwCasingColor,
                start = Offset(width * 0.30f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.42f + panOffsetX, height + panOffsetY),
                strokeWidth = 12f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = arterialColor,
                start = Offset(width * 0.30f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.42f + panOffsetX, height + panOffsetY),
                strokeWidth = 7f * zoomScale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = hwCasingColor,
                start = Offset(width * 0.72f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.82f + panOffsetX, height + panOffsetY),
                strokeWidth = 12f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = arterialColor,
                start = Offset(width * 0.72f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.82f + panOffsetX, height + panOffsetY),
                strokeWidth = 7f * zoomScale,
                cap = StrokeCap.Round
            )

            // Live Traffic Overlay (Green / Amber / Red Speed Ribbons on Hybrid Roads)
            if (showTrafficLayer) {
                // Free flow speed on US-101 south
                drawLine(
                    color = SpeedGreen,
                    start = Offset(width * 0.12f + panOffsetX, height * 0.83f + panOffsetY),
                    end = Offset(width * 0.45f + panOffsetX, height * 0.56f + panOffsetY),
                    strokeWidth = 3.5f * zoomScale,
                    cap = StrokeCap.Round
                )
                // Heavy congestion (+16 min) near Junction 28 on I-405/I-80
                drawLine(
                    color = HazardRed,
                    start = Offset(width * 0.48f + panOffsetX, height * 0.53f + panOffsetY),
                    end = Offset(width * 0.72f + panOffsetX, height * 0.34f + panOffsetY),
                    strokeWidth = 4f * zoomScale,
                    cap = StrokeCap.Round
                )
            }

            // ==========================================
            // 3. ALTERNATE & ACTIVE ROUTE POLYLINES
            // ==========================================
            // Alternate Route (Dashed Polyline in background)
            val alternatePath = Path().apply {
                moveTo(width * 0.15f + panOffsetX, height * 0.78f + panOffsetY)
                cubicTo(
                    width * 0.20f + panOffsetX, height * 0.50f + panOffsetY,
                    width * 0.45f + panOffsetX, height * 0.65f + panOffsetY,
                    width * 0.85f + panOffsetX, height * 0.18f + panOffsetY
                )
            }
            drawPath(
                path = alternatePath,
                color = Color(0xFF94A3B8).copy(alpha = 0.75f),
                style = Stroke(
                    width = 6f * zoomScale,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // Primary Active Route Polyline
            val primaryColor = when (selectedRoute?.type) {
                RouteSuggestionType.ECO_FRIENDLY -> SpeedGreen
                RouteSuggestionType.SCENIC_SMOOTH -> NeonCyan
                RouteSuggestionType.HEAVY_VEHICLE -> AlertAmber
                else -> ElectricBlue
            }

            val routePath = Path().apply {
                moveTo(width * 0.15f + panOffsetX, height * 0.78f + panOffsetY)
                cubicTo(
                    width * 0.35f + panOffsetX, height * 0.75f + panOffsetY,
                    width * 0.30f + panOffsetX, height * 0.45f + panOffsetY,
                    width * 0.50f + panOffsetX, height * 0.42f + panOffsetY
                )
                cubicTo(
                    width * 0.70f + panOffsetX, height * 0.40f + panOffsetY,
                    width * 0.65f + panOffsetX, height * 0.20f + panOffsetY,
                    width * 0.85f + panOffsetX, height * 0.18f + panOffsetY
                )
            }

            // Glow Path
            drawPath(
                path = routePath,
                color = primaryColor.copy(alpha = 0.35f),
                style = Stroke(width = 24f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Core Route Line
            drawPath(
                path = routePath,
                color = primaryColor,
                style = Stroke(width = 10f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Route Centerline
            drawPath(
                path = routePath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 2.5f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // ==========================================
            // 4. DAMAGE / POTHOLE ACCELEROMETER LOGS
            // ==========================================
            if (showDamageLayer) {
                val potholeX = width * 0.50f + panOffsetX
                val potholeY = height * 0.42f + panOffsetY

                drawCircle(
                    color = HazardRed.copy(alpha = (1f - pulsePhase) * 0.6f),
                    radius = (14f + (pulsePhase * 22f)) * zoomScale,
                    center = Offset(potholeX, potholeY)
                )
                drawCircle(
                    color = HazardRed,
                    radius = 7f * zoomScale,
                    center = Offset(potholeX, potholeY)
                )

                val roughX = width * 0.32f + panOffsetX
                val roughY = height * 0.60f + panOffsetY
                drawCircle(
                    color = AlertAmber.copy(alpha = 0.7f),
                    radius = 6f * zoomScale,
                    center = Offset(roughX, roughY)
                )
            }

            // ==========================================
            // 5. WEATHER SIMULATION LAYER
            // ==========================================
            if (showWeatherLayer) {
                val rainColor = Color(0x6638BDF8)
                for (i in 0 until 20) {
                    val rx = ((i * 57f + rainOffset * 2f) % width)
                    val ry = ((i * 37f + rainOffset * 3f) % height)
                    drawLine(
                        color = rainColor,
                        start = Offset(rx, ry),
                        end = Offset(rx - 6f, ry + 16f),
                        strokeWidth = 1.5f
                    )
                }
            }

            // ==========================================
            // 6. ORIGIN & DESTINATION PINS
            // ==========================================
            val startX = width * 0.15f + panOffsetX
            val startY = height * 0.78f + panOffsetY
            val destX = width * 0.85f + panOffsetX
            val destY = height * 0.18f + panOffsetY

            // Start Pin
            drawCircle(color = Color.White, radius = 9f * zoomScale, center = Offset(startX, startY))
            drawCircle(color = NeonCyan, radius = 6f * zoomScale, center = Offset(startX, startY))

            // Destination Pin (Double Glow)
            drawCircle(color = HazardRed.copy(alpha = 0.3f), radius = 16f * zoomScale, center = Offset(destX, destY))
            drawCircle(color = HazardRed, radius = 10f * zoomScale, center = Offset(destX, destY))
            drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(destX, destY))

            // ==========================================
            // 7. CURRENT LOCATION & VEHICLE RADAR
            // ==========================================
            val carT = if (isNavigating) ((navStep.toFloat() / 5f) + (pulsePhase * 0.08f)).coerceIn(0f, 0.95f) else 0.42f
            val carX = width * (0.15f + (0.70f * carT)) + panOffsetX
            val carY = height * (0.78f - (0.60f * carT)) + panOffsetY

            // Current Location Outer Pulse Radar Halo
            drawCircle(
                color = NeonCyan.copy(alpha = (1f - pulsePhase) * 0.6f),
                radius = (22f + (pulsePhase * 32f)) * zoomScale,
                center = Offset(carX, carY)
            )
            drawCircle(
                color = ElectricBlue.copy(alpha = 0.35f),
                radius = 18f * zoomScale,
                center = Offset(carX, carY)
            )
            // Current Location Chassis Marker
            drawCircle(color = Color(0xFF0B132B), radius = 14f * zoomScale, center = Offset(carX, carY))
            drawCircle(color = NeonCyan, radius = 9f * zoomScale, center = Offset(carX, carY))
            drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(carX, carY))

            // Directional Heading Indicator Tip
            val headingPath = Path().apply {
                moveTo(carX, carY - (17f * zoomScale))
                lineTo(carX - (6f * zoomScale), carY - (8f * zoomScale))
                lineTo(carX + (6f * zoomScale), carY - (8f * zoomScale))
                close()
            }
            drawPath(path = headingPath, color = SpeedGreen)
        }

        // ==========================================
        // TOP-LEFT STATUS & CURRENT LOCATION BADGE
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xD9071524),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isNavigating) SpeedGreen else NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mapTheme.badge,
                        color = if (isNavigating) SpeedGreen else NeonCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Prominent Current Location Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xE6071524),
                border = BorderStroke(1.dp, NeonCyan)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Current Location",
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "📍 $currentLocationName",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPS: $currentGpsCoords • Satellite Fix: 12 Satellites",
                            color = NeonCyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weather & Road Grip Badge
            if (showWeatherLayer) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xC0071524),
                    border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = weatherCondition,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ==========================================
        // TOP-RIGHT LAYER & MAP THEME CONTROLS
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Map Theme Switcher Button (Satellite / Roadmap / Cyber)
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE6071524),
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.clickable { showLayerMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = "Map Style",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (mapTheme) {
                                    MapThemeMode.SATELLITE_HYBRID -> "Satellite"
                                    MapThemeMode.ROADMAP -> "Roadmap"
                                    MapThemeMode.CYBER_HUD -> "HUD"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false },
                        modifier = Modifier.background(Color(0xFF0F172A))
                    ) {
                        MapThemeMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        mode.displayName,
                                        color = if (mapTheme == mode) NeonCyan else Color.White,
                                        fontWeight = if (mapTheme == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    mapTheme = mode
                                    showLayerMenu = false
                                }
                            )
                        }
                    }
                }

                // Traffic Flow Toggle
                Surface(
                    shape = CircleShape,
                    color = if (showTrafficLayer) SpeedGreen.copy(alpha = 0.25f) else Color(0xCC071524),
                    border = BorderStroke(1.dp, if (showTrafficLayer) SpeedGreen else Color(0xFF334155)),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showTrafficLayer = !showTrafficLayer }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Traffic,
                            contentDescription = "Toggle Traffic Flow",
                            tint = if (showTrafficLayer) SpeedGreen else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Weather Layer Toggle
                Surface(
                    shape = CircleShape,
                    color = if (showWeatherLayer) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xCC071524),
                    border = BorderStroke(1.dp, if (showWeatherLayer) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showWeatherLayer = !showWeatherLayer }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = "Toggle Weather",
                            tint = if (showWeatherLayer) Color(0xFF38BDF8) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Damage Score Layer Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (showDamageLayer) HazardRed.copy(alpha = 0.25f) else Color(0xCC071524),
                border = BorderStroke(1.dp, if (showDamageLayer) HazardRed else Color(0xFF334155)),
                modifier = Modifier.clickable { showDamageLayer = !showDamageLayer }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Pothole Damage Layer",
                        tint = if (showDamageLayer) HazardRed else Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Potholes ($damageScore/100)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showDamageLayer) HazardRed else Color.Gray
                    )
                }
            }

            // Snap to Current Location Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xE6071524),
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier.clickable {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    zoomScale = 1.0f
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Snap to Current Location",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recenter",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        // ==========================================
        // BOTTOM HUD: LIVE OBD-II TELEMETRY RIBBON
        // ==========================================
        if (showTelemetryHud) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEA071524),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPEED", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${telemetry.speedMph} MPH", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RPM", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${telemetry.rpm}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COOLANT", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text(
                            "${telemetry.coolantTempF}°F",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.coolantTempF > 220) HazardRed else SpeedGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOAD", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${telemetry.engineLoadPercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BATTERY", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("${telemetry.batteryVoltage}V", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPro)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BLE STATUS", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(SpeedGreen, CircleShape))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("ELM327", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpeedGreen)
                        }
                    }
                }
            }
        }
    }
}
