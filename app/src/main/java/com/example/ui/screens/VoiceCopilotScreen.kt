package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSender
import com.example.data.model.ChatbotPersona
import com.example.data.model.GeminiAiModel
import com.example.data.model.GroundedMapPlace
import com.example.data.model.GroundingCitation
import com.example.data.model.LiveVoiceSessionState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun VoiceCopilotScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val selectedModel by viewModel.selectedGeminiModel.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val searchGrounding by viewModel.searchGroundingEnabled.collectAsState()
    val mapsGrounding by viewModel.mapsGroundingEnabled.collectAsState()
    val liveVoiceState by viewModel.liveVoiceState.collectAsState()
    val waveformEnergy by viewModel.audioWaveformEnergy.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val isSpeaking by viewModel.isVoiceSpeaking.collectAsState()
    val isProcessing by viewModel.isChatProcessing.collectAsState()
    val user by viewModel.userAccount.collectAsState()

    val isSttListening by viewModel.isSttListening.collectAsState()
    val sttTranscript by viewModel.sttTranscript.collectAsState()
    val sttError by viewModel.sttError.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var selectedCopilotMode by remember { mutableIntStateOf(0) } // 0: Multi-Turn Chat, 1: Live Voice API
    val listState = rememberLazyListState()

    // Permission launcher for Speech Recognition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSpeechToText { recognizedText ->
                viewModel.sendChatMessage(recognizedText, isVoice = true)
            }
        }
    }

    val quickPrompts = listOf(
        "Scan OBD codes from ELM327 BLE ⚡",
        "Recommend alternate route to avoid potholes & mitigate damage 🛡️",
        "What is my vehicle info, condition & coolant temp? 🚗",
        "Weather impact & road grip conditions on route 🌧️",
        "GPS turn-by-turn directions to Silicon Valley Hub 🗺️",
        "Explain DTC P0300 misfire cause & estimated repair cost 🔧",
        "Clear Check Engine Light & reset ECU ⚠️"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speech-To-Text (STT) Active Listening Dialog
    if (isSttListening) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSpeechToText() },
            containerColor = Color(0xFF0E1A38),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speech-to-Text Listening...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeonCyan)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    // Audio waveform visualization circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((70 * (1f + (waveformEnergy * 0.4f))).dp)
                                .background(NeonCyan.copy(alpha = 0.25f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(ElectricBlue, NeonCyan)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Listening", tint = Color.Black, modifier = Modifier.size(30.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (sttTranscript.isNotBlank()) "\"$sttTranscript\"" else "Speak your question (e.g. 'What is my current engine status?')...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (sttTranscript.isNotBlank()) Color.White else Color(0xFF8D99AE),
                        lineHeight = 20.sp
                    )

                    if (sttError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sttError ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopSpeechToText()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedGreen),
                    modifier = Modifier.testTag("stt_dialog_done_button")
                ) {
                    Text("Send Query", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelSpeechToText() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8D99AE))
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B19))
    ) {
        // Mode Header Tabs
        TabRow(
            selectedTabIndex = selectedCopilotMode,
            containerColor = Color(0xFF0B132B),
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCopilotMode]),
                    color = NeonCyan,
                    height = 3.dp
                )
            },
            modifier = Modifier.testTag("copilot_mode_tabs")
        ) {
            Tab(
                selected = selectedCopilotMode == 0,
                onClick = { selectedCopilotMode = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gemini Multi-Turn Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = NeonCyan,
                unselectedContentColor = Color(0xFF8D99AE)
            )
            Tab(
                selected = selectedCopilotMode == 1,
                onClick = {
                    selectedCopilotMode = 1
                    if (liveVoiceState == LiveVoiceSessionState.DISCONNECTED) {
                        viewModel.startLiveVoiceSession()
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Voice Co-Pilot (Live API)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = SpeedGreen,
                unselectedContentColor = Color(0xFF8D99AE)
            )
        }

        if (selectedCopilotMode == 1) {
            // Live Voice Co-Pilot Screen (gemini-3.1-flash-live-preview)
            LiveVoiceCopilotView(
                viewModel = viewModel,
                liveVoiceState = liveVoiceState,
                waveformEnergy = waveformEnergy,
                liveTranscript = liveTranscript,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing
            )
        } else {
            // Multi-Turn Chatbot Screen
            MultiTurnChatbotView(
                viewModel = viewModel,
                messages = messages,
                selectedModel = selectedModel,
                selectedPersona = selectedPersona,
                searchGrounding = searchGrounding,
                mapsGrounding = mapsGrounding,
                isProcessing = isProcessing,
                isSttListening = isSttListening,
                textInput = textInput,
                onTextInputChange = { textInput = it },
                onStartStt = {
                    val hasRecordPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasRecordPerm) {
                        viewModel.startSpeechToText { recognizedText ->
                            viewModel.sendChatMessage(recognizedText, isVoice = true)
                        }
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(textInput, isVoice = false)
                        textInput = ""
                    }
                },
                quickPrompts = quickPrompts,
                listState = listState,
                userRemainingTokens = if (user.tier.chatTokensPerDay == -1) "Unlimited" else "${(user.tier.chatTokensPerDay - user.chatTokensUsedToday).coerceAtLeast(0)} left"
            )
        }
    }
}

@Composable
fun MultiTurnChatbotView(
    viewModel: MainViewModel,
    messages: List<ChatMessage>,
    selectedModel: GeminiAiModel,
    selectedPersona: ChatbotPersona,
    searchGrounding: Boolean,
    mapsGrounding: Boolean,
    isProcessing: Boolean,
    isSttListening: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onStartStt: () -> Unit,
    onSend: () -> Unit,
    quickPrompts: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    userRemainingTokens: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // AI Controls & Persona Configuration Bar
        Surface(
            color = Color(0xFF0E1A38),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Persona Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ROLE PERSONA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8D99AE),
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.size(28.dp).testTag("clear_chat_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat History",
                            tint = Color(0xFF8D99AE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).testTag("persona_selector_row")
                ) {
                    items(ChatbotPersona.values()) { persona ->
                        val isSelected = persona == selectedPersona
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setChatPersona(persona) },
                            label = { Text(persona.shortName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (persona) {
                                        ChatbotPersona.MECHANIC -> Icons.Default.Build
                                        ChatbotPersona.NAVIGATOR -> Icons.Default.Navigation
                                        ChatbotPersona.CARGO -> Icons.Default.LocalShipping
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyan,
                                selectedLeadingIconColor = NeonCyan,
                                containerColor = Color(0xFF152244),
                                labelColor = Color(0xFFB0C4DE),
                                iconColor = Color(0xFF8D99AE)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NeonCyan else Color(0xFF263868)
                            )
                        )
                    }
                }

                // Model Selection Chips
                Text(
                    text = "GEMINI MODEL SELECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8D99AE),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).testTag("model_selector_row")
                ) {
                    items(listOf(GeminiAiModel.FLASH, GeminiAiModel.PRO_PREVIEW, GeminiAiModel.FLASH_LITE)) { model ->
                        val isSelected = model == selectedModel
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setGeminiModel(model) },
                            label = {
                                Column {
                                    Text(model.id, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(model.badge, fontSize = 9.sp, color = if (isSelected) NeonCyan else Color(0xFF8D99AE))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF131D38),
                                labelColor = Color(0xFFB0C4DE)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ElectricBlue else Color(0xFF263868)
                            )
                        )
                    }
                }

                // Gemini Female Voice Selector
                val currentFemaleVoice by viewModel.selectedFemaleVoice.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GEMINI FEMALE VOICE (TTS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8D99AE),
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { viewModel.playFemaleVoicePreview(currentFemaleVoice) },
                        modifier = Modifier.size(24.dp).testTag("preview_female_voice_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Test Female Voice", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).testTag("female_voice_selector_row")
                ) {
                    items(com.example.data.repository.GeminiFemaleVoice.values()) { voice ->
                        val isSelected = voice == currentFemaleVoice
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFemaleVoice(voice) },
                            label = { Text(voice.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isSelected) SpeedGreen else Color(0xFF8D99AE))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpeedGreen.copy(alpha = 0.2f),
                                selectedLabelColor = SpeedGreen,
                                containerColor = Color(0xFF131D38),
                                labelColor = Color(0xFFB0C4DE)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) SpeedGreen else Color(0xFF263868)
                            )
                        )
                    }
                }

                // Grounding Controls (Search & Maps)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Search Grounding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleSearchGrounding(!searchGrounding) }
                            .padding(4.dp)
                            .testTag("toggle_search_grounding")
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = if (searchGrounding) NeonCyan else Color(0xFF8D99AE),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Google Search",
                            fontSize = 11.sp,
                            color = if (searchGrounding) Color.White else Color(0xFF8D99AE),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = searchGrounding,
                            onCheckedChange = { viewModel.toggleSearchGrounding(it) },
                            modifier = Modifier.size(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Google Maps Grounding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleMapsGrounding(!mapsGrounding) }
                            .padding(4.dp)
                            .testTag("toggle_maps_grounding")
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (mapsGrounding) SpeedGreen else Color(0xFF8D99AE),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Google Maps",
                            fontSize = 11.sp,
                            color = if (mapsGrounding) Color.White else Color(0xFF8D99AE),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = mapsGrounding,
                            onCheckedChange = { viewModel.toggleMapsGrounding(it) },
                            modifier = Modifier.size(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpeedGreen,
                                checkedTrackColor = SpeedGreen.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        // Messages Thread (Scrollable)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("chat_messages_stream"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message, onPlayTts = { viewModel.voiceRepo.speak(it) })
            }

            if (isProcessing) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gemini is analyzing vehicle telemetry & live highway streams...",
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Quick Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B132B))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("quick_driver_prompts")
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    color = Color(0xFF18284F),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E467C)),
                    modifier = Modifier.clickable {
                        viewModel.sendChatMessage(prompt, isVoice = false)
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = Color(0xFFD0E0FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Input Row
        Surface(
            color = Color(0xFF0E1A38),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    placeholder = { Text("Ask GPS Co-Pilot or DTC...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF152244),
                        unfocusedContainerColor = Color(0xFF152244),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF263868)
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Speech-to-Text Voice Query Button
                IconButton(
                    onClick = onStartStt,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isSttListening) Color.Red else Color(0xFF1C2B54),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSttListening) Color.Red else NeonCyan,
                            shape = CircleShape
                        )
                        .testTag("speech_to_text_mic_button")
                ) {
                    Icon(
                        if (isSttListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Speech-to-Text",
                        tint = if (isSttListening) Color.White else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onSend,
                    enabled = textInput.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(ElectricBlue, NeonCyan)),
                            shape = CircleShape
                        )
                        .testTag("send_chat_message_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onPlayTts: (String) -> Unit
) {
    val isUser = message.sender == ChatSender.USER
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Metadata / Model info header for assistant
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${message.personaUsed ?: "Master Mechanic"} • ${message.modelUsed ?: "gemini-3.5-flash"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        Surface(
            color = if (isUser) Color(0xFF1D4ED8) else Color(0xFF131F3F),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) BorderStroke(1.dp, Color(0xFF283B68)) else null,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                // Google Search Grounding Citations
                if (message.searchCitations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "🔍 SEARCH GROUNDING SOURCES:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.searchCitations.forEach { citation ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0E172F))
                                    .clickable {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(citation.url))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            // Handle exception
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(citation.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(citation.url, color = Color(0xFF8D99AE), fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Google Maps Grounding Places
                if (message.groundedPlaces.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "📍 GROUNDED GOOGLE MAPS LOCATIONS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpeedGreen,
                        letterSpacing = 0.5.sp
                    )
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.groundedPlaces.forEach { place ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A33)),
                                border = BorderStroke(1.dp, Color(0xFF1E3A68)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(place.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = GoldPro, modifier = Modifier.size(12.dp))
                                            Text(" ${place.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPro)
                                        }
                                    }
                                    Text(place.category, fontSize = 10.sp, color = SpeedGreen, fontWeight = FontWeight.SemiBold)
                                    Text(place.address, fontSize = 10.sp, color = Color(0xFFB0C4DE), modifier = Modifier.padding(vertical = 2.dp))
                                    Text(place.openStatus, fontSize = 9.sp, color = Color(0xFF8D99AE))
                                }
                            }
                        }
                    }
                }

                // Audio Playback button for Assistant
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onPlayTts(message.content) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = Color(0xFF8D99AE),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveVoiceCopilotView(
    viewModel: MainViewModel,
    liveVoiceState: LiveVoiceSessionState,
    waveformEnergy: Float,
    liveTranscript: String,
    isSpeaking: Boolean,
    isProcessing: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_wave")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Model Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A38)),
            border = BorderStroke(1.dp, Color(0xFF263868)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (liveVoiceState != LiveVoiceSessionState.DISCONNECTED) SpeedGreen else Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LIVE VOICE API ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpeedGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Model: gemini-3.1-flash-live-preview",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Visualizer / Waveform Centerpiece
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size((140 * (1f + (waveformEnergy * 0.35f))).dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    SpeedGreen.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Mid Waveform Ring
                Box(
                    modifier = Modifier
                        .size((110 * (1f + (waveformEnergy * 0.2f))).dp)
                        .border(
                            2.dp,
                            brush = Brush.sweepGradient(listOf(SpeedGreen, NeonCyan, ElectricBlue, SpeedGreen)),
                            shape = CircleShape
                        )
                )

                // Core Voice Node
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                if (isSpeaking) listOf(SpeedGreen, NeonCyan) else listOf(ElectricBlue, NeonCyan)
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            if (liveVoiceState == LiveVoiceSessionState.LISTENING) {
                                viewModel.stopListeningAndSend()
                            } else {
                                viewModel.startListeningLive()
                            }
                        }
                        .testTag("live_voice_orb_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.Mic,
                        contentDescription = "Live Voice Orb",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (liveVoiceState) {
                    LiveVoiceSessionState.CONNECTING -> "Handshaking with Gemini Live Preview..."
                    LiveVoiceSessionState.LISTENING -> "Listening to speech... (Tap orb to finish)"
                    LiveVoiceSessionState.PROCESSING -> "Gemini Live synthesizing audio response..."
                    LiveVoiceSessionState.SPEAKING -> "Co-Pilot is speaking..."
                    LiveVoiceSessionState.CONNECTED_IDLE -> "Connected & Ready • Tap Mic to Speak"
                    else -> "Live session disconnected"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when (liveVoiceState) {
                    LiveVoiceSessionState.LISTENING -> NeonCyan
                    LiveVoiceSessionState.SPEAKING -> SpeedGreen
                    else -> Color.White
                }
            )
        }

        // Live Transcript Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D3B)),
            border = BorderStroke(1.dp, Color(0xFF263868)),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "REAL-TIME LIVE TRANSCRIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8D99AE),
                        letterSpacing = 1.sp
                    )
                    if (isSpeaking) {
                        IconButton(onClick = { viewModel.stopSpeaking() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Interrupt Voice", tint = Color.Red)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (liveTranscript.isNotBlank()) liveTranscript else "Say 'Hey GPS', 'Check engine diagnostics', or 'What's the best route to the airport?'",
                    fontSize = 13.sp,
                    color = if (liveTranscript.isNotBlank()) Color.White else Color(0xFF6B7280),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Simulated Voice Scenarios
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "QUICK VOICE CO-PILOT SIMULATIONS:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8D99AE),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.stopListeningAndSend("What is my current engine status?") },
                    modifier = Modifier.weight(1f).testTag("quick_voice_engine_status"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Text("Engine Status 🚗", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.stopListeningAndSend("Hey Route Logic, check engine error codes and battery voltage.") },
                    modifier = Modifier.weight(1f).testTag("quick_voice_diagnostics"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedGreen)
                ) {
                    Text("OBD-II Scan ⚠️", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls Bottom Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (liveVoiceState == LiveVoiceSessionState.DISCONNECTED) {
                        viewModel.startLiveVoiceSession()
                    } else {
                        viewModel.stopLiveVoiceSession()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (liveVoiceState != LiveVoiceSessionState.DISCONNECTED) Color(0xFF374151) else SpeedGreen
                ),
                modifier = Modifier.weight(1f).testTag("live_voice_toggle_session_button")
            ) {
                Text(
                    if (liveVoiceState != LiveVoiceSessionState.DISCONNECTED) "Disconnect Live Session" else "Connect Live Voice API",
                    color = if (liveVoiceState != LiveVoiceSessionState.DISCONNECTED) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
