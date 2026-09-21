package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.LiveVoiceSessionState
import com.example.data.model.RouteOption
import com.example.data.model.RouteSuggestionType
import com.example.ui.components.FloatingCopilotHudOverlay
import com.example.ui.components.MapVisualizer
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun RouteScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val origin by viewModel.originInput.collectAsState()
    val destination by viewModel.destinationInput.collectAsState()
    val routes by viewModel.routeOptions.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val aiAdvice by viewModel.aiTrafficAdvice.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val navStep by viewModel.currentNavStep.collectAsState()
    val rerouteAlert by viewModel.liveRerouteAlert.collectAsState()
    val user by viewModel.userAccount.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val isSpeaking by viewModel.isVoiceSpeaking.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val damageReport by viewModel.damageReport.collectAsState()
    val cachedOfflineRoutes by viewModel.cachedOfflineRoutes.collectAsState()

    val activeCar = vehicles.firstOrNull { it.isDefault } ?: vehicles.firstOrNull()
    val isSttListening by viewModel.isSttListening.collectAsState()
    val sttTranscript by viewModel.sttTranscript.collectAsState()
    val liveVoiceState by viewModel.liveVoiceState.collectAsState()
    val isChatProcessing by viewModel.isChatProcessing.collectAsState()

    val context = LocalContext.current
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var showVoiceCommandSheet by remember { mutableStateOf(false) }
    var lastNavigationVoiceFeedback by remember { mutableStateOf<String?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            viewModel.startSpeechToText { recognizedText ->
                viewModel.processNavigationVoiceCommand(recognizedText)
            }
        }
    }

    fun handlePushToTalkClick() {
        if (!hasRecordAudioPermission) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            if (isSttListening) {
                viewModel.stopSpeechToText()
            } else {
                viewModel.startSpeechToText { recognizedText ->
                    viewModel.processNavigationVoiceCommand(recognizedText)
                }
            }
        }
    }

    // Animation for active voice listening pulse
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ptt_scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GPS Route Logic",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                        Text(
                            text = "Real-Time Traffic & Personalized AI Guidance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SpeedGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Traffic Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedGreen
                            )
                        }
                    }
                }
            }

            // Active Vehicle Spec Pill & British Greeting Trigger
            activeCar?.let { car ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B38)),
                        border = BorderStroke(1.dp, Color(0xFF1E2D5A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${car.year} ${car.make} ${car.model} • ${car.trim}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Engine: ${car.engine} | Size: ${car.size}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.greetUserOnOpen(force = true) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("🇬🇧", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            // Origin & Destination Inputs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Start Point",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = origin,
                                onValueChange = { viewModel.updateOrigin(it) },
                                label = { Text("Starting Location", fontSize = 12.sp) },
                                placeholder = { Text("Enter starting point or tap GPS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.updateOrigin("📍 Current Location (450 Mission St, SF)") },
                                        modifier = Modifier.testTag("use_current_location_button")
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = "Use Current Location",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("origin_input_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Destination",
                                tint = HazardRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { viewModel.updateDestination(it) },
                                label = { Text("Destination", fontSize = 12.sp) },
                                placeholder = { Text("Enter destination address or landmark", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("destination_input_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.loadOfflineCachedRoutes() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, SpeedGreen.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("load_offline_routes_button")
                            ) {
                                Icon(Icons.Default.Route, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Offline Cache (${cachedOfflineRoutes.size})", color = SpeedGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.calculateRoutes() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("find_routes_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Find Routes", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Interactive Map Visualizer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    MapVisualizer(
                        selectedRoute = selectedRoute,
                        alternateRoutes = routes,
                        isNavigating = isNavigating,
                        navStep = navStep,
                        telemetry = telemetry,
                        weatherCondition = "Wet Asphalt • Rain 68°F",
                        damageScore = damageReport.overallScore,
                        onSelectAlternateRoute = { viewModel.selectRoute(it) }
                    )
                }
            }

            // AI Traffic & Lane Optimization Tip Card
            if (!aiAdvice.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13233D)),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Tip",
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Traffic Intelligence",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = aiAdvice ?: "",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: Personalized Route Options
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personalized Route Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${routes.size} Paths",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List of 4 Route Cards
            items(routes) { route ->
                val isSelected = selectedRoute?.id == route.id
                val typeColor = when (route.type) {
                    RouteSuggestionType.FASTEST -> ElectricBlue
                    RouteSuggestionType.ECO_FRIENDLY -> SpeedGreen
                    RouteSuggestionType.SCENIC_SMOOTH -> NeonCyan
                    RouteSuggestionType.HEAVY_VEHICLE -> AlertAmber
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectRoute(route) }
                        .testTag("route_option_${route.type.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) typeColor else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = typeColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (route.type) {
                                                RouteSuggestionType.FASTEST -> Icons.Default.FastForward
                                                RouteSuggestionType.ECO_FRIENDLY -> Icons.Default.Eco
                                                RouteSuggestionType.SCENIC_SMOOTH -> Icons.Default.Landscape
                                                RouteSuggestionType.HEAVY_VEHICLE -> Icons.Default.LocalShipping
                                            },
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = route.type.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = route.summary,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = typeColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = route.type.badge,
                                    color = typeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid (ETA, Distance, Fuel, Traffic Delay)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ETA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${route.durationMinutes} min", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = typeColor)
                            }
                            Column {
                                Text("Distance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${route.distanceMiles} mi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Traffic", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (route.trafficDelayMinutes > 0) "+${route.trafficDelayMinutes}m delay" else "Clear Flow",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (route.trafficDelayMinutes > 0) AlertAmber else SpeedGreen
                                )
                            }
                            Column {
                                Text("Eco Benefit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (route.fuelSavingsPercent > 0) "+${route.fuelSavingsPercent}% MPG" else "Standard",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = SpeedGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 ${route.personalizedReason}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons (Start Navigation / Push-to-Talk Voice Co-Pilot / Save)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.startNavigating() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("start_navigation_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start GPS Guidance", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }

                        // Push-to-Talk Gemini Co-Pilot Voice Button
                        Button(
                            onClick = { handlePushToTalkClick() },
                            modifier = Modifier
                                .height(52.dp)
                                .scale(if (isSttListening) pulseScale else 1f)
                                .testTag("push_to_talk_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSttListening) HazardRed else Color(0xFF15264B)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSttListening) HazardRed else NeonCyan
                            )
                        ) {
                            Icon(
                                imageVector = if (isSttListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isSttListening) "Listening - Tap to Stop" else "Push-to-Talk Voice Command",
                                tint = if (isSttListening) Color.White else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSttListening) "Listening..." else "PTT Mic",
                                color = if (isSttListening) Color.White else NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.saveCurrentRoute() },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("save_route_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Save Route", tint = NeonCyan)
                        }
                    }

                    // Voice Command Helper Sheet / Quick Prompts Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSttListening) "🔴 Speak navigation or diagnostic command..." else "Gemini Live Navigation Voice Ready",
                            fontSize = 11.sp,
                            color = if (isSttListening) HazardRed else SpeedGreen,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF131F3B),
                            border = BorderStroke(1.dp, Color(0xFF233660)),
                            modifier = Modifier.clickable { showVoiceCommandSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voice Commands", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Ad-Supported Free Banner simulator
            if (user.tier.hasAds) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                        border = BorderStroke(1.dp, GoldPro.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sponsored • AutoZone OBD Adapters",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPro
                                )
                                Text(
                                    text = "Upgrade to Premium for an Ad-Free GPS experience & 10 AI Scans/day.",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                            Button(
                                onClick = { viewModel.showPaywall(true) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPro),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("Remove Ads", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Turn-by-Turn Navigation Co-Pilot HUD Overlay
        FloatingCopilotHudOverlay(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Active Voice Recognition & Co-Pilot Response Floating Overlay
        AnimatedVisibility(
            visible = isSttListening || isChatProcessing || sttTranscript.isNotBlank(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (rerouteAlert != null) 160.dp else 80.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_copilot_feedback_banner"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF20B132B),
                border = BorderStroke(1.5.dp, if (isSttListening) HazardRed else NeonCyan),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSttListening) HazardRed.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f))
                            .scale(if (isSttListening) pulseScale else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChatProcessing) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isSttListening) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isSttListening) HazardRed else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSttListening) "VOICE COMMAND LISTENING..." else if (isChatProcessing) "GEMINI PROCESSING..." else "GEMINI CO-PILOT RESPONSE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSttListening) HazardRed else NeonCyan
                        )
                        Text(
                            text = if (isSttListening) (sttTranscript.ifBlank { "Speak: 'Reroute around traffic' or 'Check tire pressure'..." }) else (sttTranscript.ifBlank { "Command executed" }),
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 2
                        )
                    }

                    if (isSttListening) {
                        IconButton(
                            onClick = { viewModel.stopSpeechToText() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Live Reroute Suggestion Popup
        AnimatedVisibility(
            visible = rerouteAlert != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xF21C2541),
                border = BorderStroke(2.dp, AlertAmber),
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Traffic Bypass Available!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AlertAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rerouteAlert ?: "",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.dismissReroute() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.testTag("dismiss_reroute_button")
                        ) {
                            Text("Dismiss", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.acceptReroute() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("accept_reroute_button")
                        ) {
                            Text("Accept Bypass", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Voice Command Cheat Sheet Dialog
        if (showVoiceCommandSheet) {
            AlertDialog(
                onDismissRequest = { showVoiceCommandSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Co-Pilot Voice Commands",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tap the 'PTT Mic' button or press any command below to speak naturally with the Automotive Specialist Co-Pilot:",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )

                        val sampleCommands = listOf(
                            "Reroute around traffic" to "Checks alternate GPS routes based on trip logs & highway density",
                            "Check engine diagnostics" to "Scans OBD2 fault codes & ECU live data",
                            "Calculate eco route" to "Finds the most fuel-efficient trajectory",
                            "Is it safe to drive with current DTCs?" to "Diagnoses vehicle severity and safety margin",
                            "Recommend alternate route" to "Suggests route based on logged telemetry & damage score"
                        )

                        sampleCommands.forEach { (cmd, desc) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131D36),
                                border = BorderStroke(1.dp, Color(0xFF233660)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showVoiceCommandSheet = false
                                        viewModel.processNavigationVoiceCommand(cmd)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "🗣️ \"$cmd\"",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showVoiceCommandSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }
    }
}
