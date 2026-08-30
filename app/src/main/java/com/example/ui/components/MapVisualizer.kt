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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

enum class MapThemeMode {
    CYBER_HUD,
    SATELLITE_HYBRID,
    DAY_CLEAN
}

@Composable
fun MapVisualizer(
    modifier: Modifier = Modifier,
    selectedRoute: RouteOption?,
    alternateRoutes: List<RouteOption> = emptyList(),
    isNavigating: Boolean = false,
    navStep: Int = 0,
    telemetry: LiveTelemetry = LiveTelemetry(),
    weatherCondition: String = "Wet Asphalt • Rain 68°F",
    damageScore: Int = 94,
    onSelectAlternateRoute: ((RouteOption) -> Unit)? = null
) {
    var showDamageLayer by remember { mutableStateOf(true) }
    var showWeatherLayer by remember { mutableStateOf(true) }
    var showTelemetryHud by remember { mutableStateOf(true) }
    var mapTheme by remember { mutableStateOf(MapThemeMode.CYBER_HUD) }
    var isExpanded by remember { mutableStateOf(false) }

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
                    MapThemeMode.CYBER_HUD -> Color(0xFF0B132B)
                    MapThemeMode.SATELLITE_HYBRID -> Color(0xFF07111E)
                    MapThemeMode.DAY_CLEAN -> Color(0xFF1E293B)
                }
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.8f, 2.5f)
                    panOffsetX = (panOffsetX + pan.x).coerceIn(-200f, 200f)
                    panOffsetY = (panOffsetY + pan.y).coerceIn(-200f, 200f)
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

            // 1. GPS Coordinate & Topo Grid
            val gridColor = when (mapTheme) {
                MapThemeMode.CYBER_HUD -> Color(0xFF1C2A4A)
                MapThemeMode.SATELLITE_HYBRID -> Color(0xFF112233)
                MapThemeMode.DAY_CLEAN -> Color(0xFF334155)
            }
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

            // 2. Base Arterial Highway Network
            val roadBedColor = Color(0xFF1E293B)
            val highwayColor = Color(0xFF334155)

            // Highway I-80 East/West
            drawLine(
                color = roadBedColor,
                start = Offset(0f + panOffsetX, height * 0.38f + panOffsetY),
                end = Offset(width + panOffsetX, height * 0.44f + panOffsetY),
                strokeWidth = 24f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = highwayColor,
                start = Offset(0f + panOffsetX, height * 0.38f + panOffsetY),
                end = Offset(width + panOffsetX, height * 0.44f + panOffsetY),
                strokeWidth = 16f * zoomScale,
                cap = StrokeCap.Round
            )

            // North-South Connector
            drawLine(
                color = highwayColor,
                start = Offset(width * 0.30f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.42f + panOffsetX, height + panOffsetY),
                strokeWidth = 14f * zoomScale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = highwayColor,
                start = Offset(width * 0.72f + panOffsetX, 0f + panOffsetY),
                end = Offset(width * 0.82f + panOffsetX, height + panOffsetY),
                strokeWidth = 14f * zoomScale,
                cap = StrokeCap.Round
            )

            // Expressway Diagonal Bypass (Highway 101)
            drawLine(
                color = Color(0xFF243352),
                start = Offset(width * 0.10f + panOffsetX, height * 0.85f + panOffsetY),
                end = Offset(width * 0.90f + panOffsetX, height * 0.20f + panOffsetY),
                strokeWidth = 12f * zoomScale,
                cap = StrokeCap.Round
            )

            // 3. Alternate Routes (Dashed Polyline in background)
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
                color = Color(0xFF64748B).copy(alpha = 0.6f),
                style = Stroke(
                    width = 6f * zoomScale,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // 4. Primary Active Route Polyline
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
                color = primaryColor.copy(alpha = 0.25f),
                style = Stroke(width = 22f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Core Route Line
            drawPath(
                path = routePath,
                color = primaryColor,
                style = Stroke(width = 9f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Accelerometer Logged Road Damage / Pothole Shock Zones
            if (showDamageLayer) {
                // Pothole cluster point on I-80
                val potholeX = width * 0.50f + panOffsetX
                val potholeY = height * 0.42f + panOffsetY

                // Pothole 2.1G Shock Ring
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

                // Vibration / Rough Asphalt segment
                val roughX = width * 0.32f + panOffsetX
                val roughY = height * 0.60f + panOffsetY
                drawCircle(
                    color = AlertAmber.copy(alpha = 0.7f),
                    radius = 6f * zoomScale,
                    center = Offset(roughX, roughY)
                )
            }

            // 6. Weather Layer (Rain effect simulation)
            if (showWeatherLayer) {
                val rainColor = Color(0x5538BDF8)
                for (i in 0 until 18) {
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

            // 7. Origin & Destination Waypoint Pins
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

            // 8. Vehicle Position Marker & Pulse
            val carT = if (isNavigating) ((navStep.toFloat() / 5f) + (pulsePhase * 0.08f)).coerceIn(0f, 0.95f) else 0.42f
            val carX = width * (0.15f + (0.70f * carT)) + panOffsetX
            val carY = height * (0.78f - (0.60f * carT)) + panOffsetY

            // Vehicle Pulse Radar
            drawCircle(
                color = NeonCyan.copy(alpha = (1f - pulsePhase) * 0.5f),
                radius = (16f + (pulsePhase * 24f)) * zoomScale,
                center = Offset(carX, carY)
            )
            // Vehicle Chassis Marker
            drawCircle(color = Color(0xFF0B132B), radius = 14f * zoomScale, center = Offset(carX, carY))
            drawCircle(color = NeonCyan, radius = 9f * zoomScale, center = Offset(carX, carY))
            drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(carX, carY))
        }

        // Top-Left Navigation Maneuver & Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xD90B132B),
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
                        text = if (isNavigating) "GPS GUIDANCE ACTIVE" else "LIVE CAN-BUS MAP",
                        color = if (isNavigating) SpeedGreen else NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weather & Road Grip Badge
            if (showWeatherLayer) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xC01E293B),
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

        // Top-Right Layer Control Buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Damage Score Layer Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (showDamageLayer) HazardRed.copy(alpha = 0.25f) else Color(0xCC0B132B),
                border = BorderStroke(1.dp, if (showDamageLayer) HazardRed else Color(0xFF334155)),
                modifier = Modifier.clickable { showDamageLayer = !showDamageLayer }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Pothole Damage Layer",
                        tint = if (showDamageLayer) HazardRed else Color.Gray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Potholes ($damageScore/100)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showDamageLayer) HazardRed else Color.Gray
                    )
                }
            }

            // Weather Layer Toggle
            Surface(
                shape = CircleShape,
                color = if (showWeatherLayer) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xCC0B132B),
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

        // Bottom HUD: Live ELM327 Telemetry Ribbon (RPM, MPH, Coolant, Load, Battery)
        if (showTelemetryHud) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEA0B132B),
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
