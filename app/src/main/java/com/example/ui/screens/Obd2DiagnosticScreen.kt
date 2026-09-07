package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DtcCode
import com.example.data.model.ScanSeverity
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

enum class DtcCategoryFilter(val label: String, val prefix: String?) {
    ALL("All Codes", null),
    ACTIVE("Active Logged", "ACTIVE"),
    POWERTRAIN("Powertrain (P)", "P"),
    CHASSIS("Chassis (C)", "C"),
    BODY("Body (B)", "B"),
    NETWORK("Network (U)", "U")
}

@Composable
fun Obd2DiagnosticScreen(
    viewModel: MainViewModel,
    onNavigateToVoice: () -> Unit = {}
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val latestScan by viewModel.latestScan.collectAsState()
    val selectedDtc by viewModel.selectedDtc.collectAsState()
    val user by viewModel.userAccount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DtcCategoryFilter.ALL) }
    var expandedCodeId by remember { mutableStateOf<String?>(null) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var customLookupCode by remember { mutableStateOf("") }
    var customLookupResult by remember { mutableStateOf<String?>(null) }
    var isLookingUpCustomCode by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Parse active scan DTCs if available
    val activeScanDtcs = remember(latestScan) {
        latestScan?.let { viewModel.parseDtcCodes(it.dtcCodesJson) } ?: emptyList()
    }

    // Filter DTC database
    val filteredDtcs = remember(searchQuery, selectedCategory, activeScanDtcs, viewModel.allSampleDtcCodes) {
        val baseList = if (selectedCategory == DtcCategoryFilter.ACTIVE) {
            activeScanDtcs
        } else {
            // Combine active and sample database, prioritizing active
            val activeCodes = activeScanDtcs.map { it.code }.toSet()
            val combined = activeScanDtcs + viewModel.allSampleDtcCodes.filter { it.code !in activeCodes }
            combined
        }

        baseList.filter { dtc ->
            val matchesCategory = when (selectedCategory) {
                DtcCategoryFilter.ALL, DtcCategoryFilter.ACTIVE -> true
                DtcCategoryFilter.POWERTRAIN -> dtc.code.startsWith("P", ignoreCase = true)
                DtcCategoryFilter.CHASSIS -> dtc.code.startsWith("C", ignoreCase = true)
                DtcCategoryFilter.BODY -> dtc.code.startsWith("B", ignoreCase = true)
                DtcCategoryFilter.NETWORK -> dtc.code.startsWith("U", ignoreCase = true)
            }

            val matchesSearch = searchQuery.isBlank() ||
                dtc.code.contains(searchQuery.trim(), ignoreCase = true) ||
                dtc.description.contains(searchQuery.trim(), ignoreCase = true) ||
                dtc.system.contains(searchQuery.trim(), ignoreCase = true) ||
                dtc.symptoms.contains(searchQuery.trim(), ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("obd2_diagnostic_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // OBD-II ECU Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B38)),
                border = BorderStroke(1.5.dp, if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) HazardRed else SpeedGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.isConnected) SpeedGreen else HazardRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "OBD-II CAN BUS DIAGNOSTIC LINK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Protocol: ISO 15765-4 CAN (500 kbps)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // MIL Check Engine Light Status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) HazardRed.copy(alpha = 0.2f) else SpeedGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) HazardRed else SpeedGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) HazardRed else SpeedGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) "MIL / CEL: ON" else "MIL: OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (telemetry.milCheckEngineOn || activeScanDtcs.isNotEmpty()) HazardRed else SpeedGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ECU Quick Stats Ribbon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Fault Codes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${activeScanDtcs.size} Logged",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeScanDtcs.isEmpty()) SpeedGreen else HazardRed
                            )
                        }
                        Column {
                            Text("Coolant Temp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${telemetry.coolantTempF}°F", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Battery Voltage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${telemetry.batteryVoltage} V", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                        Column {
                            Text("OBD Readiness", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Complete", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpeedGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Diagnostic Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerAiScan(deepScan = false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("obd2_scan_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            enabled = !isScanning
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan OBD-II", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { showClearConfirmationDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("obd2_clear_codes_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HazardRed.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HazardRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = HazardRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Codes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.toggleFreezeFrameModal(true) },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(42.dp)
                                .testTag("obd2_freeze_frame_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, ElectricBlue)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Freeze", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live OBD2 Code Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("obd2_search_input"),
                placeholder = {
                    Text("Search code (e.g. P0300) or symptom (misfire, O2, ABS)...", fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DtcCategoryFilter.values()) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NeonCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Section Title: Code Display and Brief Definition Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OBD2 Diagnostic Codes & Definitions (${filteredDtcs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "SAE J2012 Standard",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List of DTC Items with Code Display & Brief Definition
        if (filteredDtcs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Fault Codes Matching Query", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching another term like 'P0', 'misfire', or 'O2 sensor'." else "The ECU reports clean diagnostic health.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredDtcs, key = { it.code }) { dtc ->
                val isExpanded = expandedCodeId == dtc.code
                val severityColor = when (dtc.severity) {
                    ScanSeverity.CRITICAL -> HazardRed
                    ScanSeverity.MODERATE -> AlertAmber
                    ScanSeverity.ADVISORY -> NeonCyan
                    ScanSeverity.HEALTHY -> SpeedGreen
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            expandedCodeId = if (isExpanded) null else dtc.code
                            viewModel.selectDtc(dtc)
                        }
                        .testTag("dtc_card_${dtc.code.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14203A)),
                    border = BorderStroke(1.2.dp, if (isExpanded) NeonCyan else severityColor.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // ROW 1: Code Display & Severity Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Monospace High-Contrast OBD2 Code Display Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = severityColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, severityColor)
                                ) {
                                    Text(
                                        text = dtc.code,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 17.sp,
                                        color = severityColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = dtc.system,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = severityColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = dtc.severity.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = severityColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PROMINENT BRIEF DEFINITION DISPLAY
                        Column {
                            Text(
                                text = "BRIEF DEFINITION:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8D99AE),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dtc.description,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 19.sp
                            )
                        }

                        // EXPANDABLE DETAILS: Symptoms, Causes, Estimated Cost, and Gemini Diagnose Action
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Common Symptoms:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertAmber
                                )
                                Text(
                                    text = dtc.symptoms,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Possible Causes:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = dtc.possibleCauses,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Estimated Repair Cost:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPro
                                        )
                                        Text(
                                            text = dtc.estimatedRepairCost,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GoldPro
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.stopListeningAndSend("Diagnose OBD-II diagnostic trouble code ${dtc.code}: ${dtc.description}. Recommend immediate mechanic fix.")
                                            viewModel.selectTab(AppTab.VOICE)
                                            onNavigateToVoice()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("ask_gemini_dtc_${dtc.code.lowercase()}")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ask Gemini AI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Recommended Fix Flowchart:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SpeedGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dtc.recommendedFix,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Instant Custom Code Lookup with Gemini AI
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162544)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instant Gemini OBD-II Code Lookup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter any SAE standard OBD-II fault code (P, C, B, or U) to query Gemini AI for instant definition and fix guidance:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customLookupCode,
                            onValueChange = { customLookupCode = it.uppercase().take(5) },
                            placeholder = { Text("e.g. P0442", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_dtc_lookup_field"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                if (customLookupCode.isNotBlank()) {
                                    isLookingUpCustomCode = true
                                    viewModel.stopListeningAndSend("Provide expert brief definition, symptoms, and priority fix for OBD-II fault code $customLookupCode.")
                                    viewModel.selectTab(AppTab.VOICE)
                                    onNavigateToVoice()
                                }
                            }),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                if (customLookupCode.isNotBlank()) {
                                    isLookingUpCustomCode = true
                                    viewModel.stopListeningAndSend("Provide expert brief definition, symptoms, and priority fix for OBD-II fault code $customLookupCode.")
                                    viewModel.selectTab(AppTab.VOICE)
                                    onNavigateToVoice()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp).testTag("custom_dtc_lookup_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Lookup", tint = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Clear Fault Codes Confirmation Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear OBD-II Trouble Codes?")
                }
            },
            text = {
                Text(
                    text = "Clearing diagnostic codes will reset the vehicle Check Engine Light (MIL) and clear freeze-frame data stored in the ECU memory.\n\nReadiness drive cycles will need to complete before emissions inspection.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEcuCodes()
                        showClearConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HazardRed)
                ) {
                    Text("Clear All Codes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
