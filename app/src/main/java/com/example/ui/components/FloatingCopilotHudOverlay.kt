package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveVoiceSessionState
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

/**
 * Floating Co-Pilot HUD Overlay that provides turn-by-turn navigation instructions
 * with seamless Gemini Voice Co-Pilot audio guidance, live vehicle speed/RPM telemetry,
 * and hands-free push-to-talk microphone triggers.
 */
@Composable
fun FloatingCopilotHudOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isNavigating by viewModel.isNavigating.collectAsState()
    val isHudEnabled by viewModel.isHudOverlayEnabled.collectAsState()
    val isMinimized by viewModel.isHudMinimized.collectAsState()
    val navStep by viewModel.currentNavStep.collectAsState()
    val activeRoute by viewModel.activeNavRoute.collectAsState()
    val isSpeaking by viewModel.isVoiceSpeaking.collectAsState()
    val isSttListening by viewModel.isSttListening.collectAsState()
    val liveVoiceState by viewModel.liveVoiceState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val currentVoice by viewModel.selectedFemaleVoice.collectAsState()
    val rerouteAlert by viewModel.liveRerouteAlert.collectAsState()

    val totalSteps = viewModel.routeRepo.turnByTurnSteps.size
    val currentStepText = viewModel.routeRepo.turnByTurnSteps.getOrElse(navStep) { "Destination Arrived" }
    val nextStepPreview = viewModel.routeRepo.turnByTurnSteps.getOrNull(navStep + 1)

    // Floating drag offset state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Pulse animation for active voice/speaking
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse_transition")
    val voiceGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse_scale"
    )

    AnimatedVisibility(
        visible = isNavigating && isHudEnabled,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(-100f, 100f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(-40f, 350f)
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("floating_copilot_hud_overlay"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF2071026), // Deep aerospace dark blue
            border = BorderStroke(
                width = if (isSpeaking || isSttListening) 2.dp else 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isSpeaking) {
                        listOf(SpeedGreen, NeonCyan, SpeedGreen)
                    } else if (isSttListening) {
                        listOf(HazardRed, GoldPro, HazardRed)
                    } else {
                        listOf(NeonCyan, ElectricBlue, NeonCyan.copy(alpha = 0.5f))
                    }
                )
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .animateContentSize()
            ) {
                // Top Status Bar: HUD Mode, Voice Indicator, Minimize/Close Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Flashing or steady Co-Pilot Icon Indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSpeaking -> SpeedGreen.copy(alpha = 0.25f)
                                        isSttListening -> HazardRed.copy(alpha = 0.25f)
                                        else -> NeonCyan.copy(alpha = 0.2f)
                                    }
                                )
                                .scale(if (isSpeaking || isSttListening) voiceGlowScale else 1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                                    isSttListening -> Icons.Default.Mic
                                    else -> Icons.Default.Navigation
                                },
                                contentDescription = "HUD Status",
                                tint = when {
                                    isSpeaking -> SpeedGreen
                                    isSttListening -> HazardRed
                                    else -> NeonCyan
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GEMINI CO-PILOT HUD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF132247)
                                ) {
                                    Text(
                                        text = currentVoice.displayName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPro,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    isSpeaking -> "Speaking turn guidance..."
                                    isSttListening -> "Listening for navigation commands..."
                                    liveVoiceState == LiveVoiceSessionState.LISTENING -> "Open Mic Active"
                                    else -> "Turn ${navStep + 1} of $totalSteps • ${activeRoute?.durationMinutes ?: 26} min ETA"
                                },
                                fontSize = 11.sp,
                                color = when {
                                    isSpeaking -> SpeedGreen
                                    isSttListening -> HazardRed
                                    else -> Color.LightGray
                                }
                            )
                        }
                    }

                    // Window controls (Minimize/Expand, Dismiss)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleHudMinimized() },
                            modifier = Modifier.size(28.dp).testTag("hud_toggle_minimize_button")
                        ) {
                            Icon(
                                imageVector = if (isMinimized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isMinimized) "Expand HUD" else "Minimize HUD",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.stopNavigating() },
                            modifier = Modifier.size(28.dp).testTag("hud_close_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Navigation",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Indicator across the route steps
                LinearProgressIndicator(
                    progress = { ((navStep + 1).toFloat() / totalSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SpeedGreen,
                    trackColor = Color(0xFF18284C)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Turn Maneuver Primary Presentation
                val turnIcon = determineTurnIcon(currentStepText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NeonCyan,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = turnIcon,
                                contentDescription = "Turn Direction",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TURN INSTRUCTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = currentStepText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp,
                            modifier = Modifier.testTag("hud_current_turn_text")
                        )
                    }

                    // Voice Re-read Action Button
                    IconButton(
                        onClick = { viewModel.speakCurrentTurnInstruction() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF132247), CircleShape)
                            .testTag("hud_speak_turn_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read Turn Instruction with Gemini Voice",
                            tint = if (isSpeaking) SpeedGreen else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // If not minimized, show secondary expanded stats (speed, next preview, PTT, advance)
                if (!isMinimized) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry & Preview Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C1733), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = SpeedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${telemetry.speedMph} MPH",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "• ${telemetry.rpm} RPM",
                                fontSize = 12.sp,
                                color = Color(0xFF8D99AE)
                            )
                        }

                        if (nextStepPreview != null) {
                            Text(
                                text = "Then: ${nextStepPreview.take(24)}...",
                                fontSize = 10.sp,
                                color = Color(0xFFA0ABC0),
                                maxLines = 1
                            )
                        }
                    }

                    // Active Reroute Alert Warning inside HUD
                    if (rerouteAlert != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF261D0F),
                            border = BorderStroke(1.dp, AlertAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Traffic Bypass Available (-5m)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertAmber
                                    )
                                }
                                Button(
                                    onClick = { viewModel.acceptReroute() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                                    contentPadding = ButtonDefaults.ContentPadding,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Accept", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // HUD Action Row (Push-to-Talk Voice & Next Maneuver)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Push-to-Talk Button with Gemini Voice Co-Pilot
                        Button(
                            onClick = {
                                if (isSttListening) {
                                    viewModel.stopSpeechToText()
                                } else {
                                    viewModel.startSpeechToText { text ->
                                        viewModel.processNavigationVoiceCommand(text)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("hud_voice_talk_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSttListening) HazardRed else Color(0xFF132247)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSttListening) HazardRed else NeonCyan
                            )
                        ) {
                            Icon(
                                imageVector = if (isSttListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Co-Pilot",
                                tint = if (isSttListening) Color.White else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSttListening) "Listening..." else "Ask Gemini",
                                color = if (isSttListening) Color.White else NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Next Turn Step Button
                        Button(
                            onClick = { viewModel.nextNavStep() },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("hud_next_maneuver_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text(
                                text = if (navStep >= totalSteps - 1) "Complete Trip" else "Next Turn ➔",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to select intuitive direction icon based on turn step keywords.
 */
private fun determineTurnIcon(stepText: String): ImageVector {
    val lower = stepText.lowercase()
    return when {
        lower.contains("left") -> Icons.Default.TurnLeft
        lower.contains("right") -> Icons.Default.TurnRight
        lower.contains("straight") || lower.contains("continue") || lower.contains("merge") -> Icons.Default.Navigation
        lower.contains("arrived") || lower.contains("destination") -> Icons.AutoMirrored.Filled.ArrowForward
        else -> Icons.AutoMirrored.Filled.ArrowForward
    }
}
