package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DamageScoreReport
import com.example.data.model.DiagnosticScanRecord
import com.example.data.model.DtcCode
import com.example.data.model.MaintenanceTask
import com.example.data.model.ScanSeverity
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val latestScan by viewModel.latestScan.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val maintenanceTasks by viewModel.maintenanceTasks.collectAsState()
    val damageReport: DamageScoreReport by viewModel.damageReport.collectAsState()
    val currentMileage by viewModel.currentVehicleMileage.collectAsState()
    val selectedDtc by viewModel.selectedDtc.collectAsState()
    val showFreezeFrame by viewModel.showFreezeFrameModal.collectAsState()
    val user by viewModel.userAccount.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    var newTaskIntervalMiles by remember { mutableStateOf("5000") }
    var newTaskIntervalMonths by remember { mutableStateOf("6") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Holographic Header Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.gps_route_logic_header),
                            contentDescription = "GPS-Route-Logic Telematics Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SpeedGreen.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, SpeedGreen)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SpeedGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OBD-II CAN LINKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpeedGreen)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cloud Run Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "GPS-Route-Logic Diagnostics",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-Time Telematics & Room Cached ECU Intelligence",
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }

            // Quota Bar
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Scan Allowance: ${user.scansUsedToday} / ${user.tier.dailyScans}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tier: ${user.tier.title}",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }

                        if (user.scansUsedToday >= user.tier.dailyScans) {
                            Button(
                                onClick = { viewModel.showPaywall(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPro),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("upgrade_quota_button")
                            ) {
                                Text("Upgrade to Pro", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SpeedGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Quota Ready",
                                    fontSize = 11.sp,
                                    color = SpeedGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Real-Time Vehicle Telematics HUD
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Telematics Gauges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CAN Bus 500kbps",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Gauges Row 1: Tachometer & Speedometer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, if (telemetry.rpm > 5000) HazardRed else NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ENGINE RPM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${telemetry.rpm}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (telemetry.rpm > 5000) HazardRed else NeonCyan
                            )
                            LinearProgressIndicator(
                                progress = { (telemetry.rpm / 6500f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (telemetry.rpm > 5000) HazardRed else NeonCyan,
                                trackColor = Color.DarkGray
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${telemetry.speedMph} MPH",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricBlue
                            )
                            LinearProgressIndicator(
                                progress = { (telemetry.speedMph / 120f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = ElectricBlue,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Gauges Row 2: Coolant Temp, Battery Voltage, Engine Load
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("COOLANT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${telemetry.coolantTempF}°F",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.coolantTempF > 218) HazardRed else SpeedGreen
                            )
                            Text("Optimal 195°F", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("BATTERY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${telemetry.batteryVoltage} V",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedGreen
                            )
                            Text("14.2V Normal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("LOAD %", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${telemetry.engineLoadPercent}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertAmber
                            )
                            Text("${telemetry.fuelEconomyMpg.toInt()} MPG", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Gauges Row 3: Intake Air Temp, Fuel Rail Pressure, Throttle %
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("INTAKE AIR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${telemetry.intakeAirTempF}°F", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Text("Ambient +10°F", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("FUEL RAIL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${telemetry.fuelRailPressurePsi} PSI", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPro)
                            Text("Direct Inject", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("THROTTLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${telemetry.throttlePercent}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                            Text("Drive-By-Wire", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Live Telematics Interactive Simulation Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Live Telematics Simulation Controls",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Button(
                                    onClick = { viewModel.simulateTelemetryThrottle(-800, -25) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Idle (800 RPM)", fontSize = 11.sp, color = NeonCyan)
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.simulateTelemetryThrottle(300, 15) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, SpeedGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Cruising (2,100 RPM)", fontSize = 11.sp, color = SpeedGreen)
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.simulateTelemetryThrottle(1200, 30) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, HazardRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("WOT Sprint (5,400 RPM)", fontSize = 11.sp, color = HazardRed)
                                }
                            }
                        }
                    }
                }
            }

            // Diagnostic Trigger Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerAiScan(deepScan = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ai_quick_scan_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Quick Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerAiScan(deepScan = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("deep_ecu_scan_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, GoldPro),
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = GoldPro)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deep ECU Scan", color = GoldPro, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Latest Scan Result Card
            if (latestScan != null) {
                val scan = latestScan!!
                val dtcList = viewModel.parseDtcCodes(scan.dtcCodesJson)
                val healthColor = if (scan.healthScore > 85) SpeedGreen else if (scan.healthScore > 60) AlertAmber else HazardRed

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                        border = BorderStroke(1.5.dp, healthColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = healthColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${scan.healthScore}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = healthColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = scan.vehicleName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${scan.severity} HEALTH INDEX",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = healthColor
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (scan.milStatus) HazardRed.copy(alpha = 0.2f) else SpeedGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (scan.milStatus) "MIL / CEL ON" else "MIL OFF",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (scan.milStatus) HazardRed else SpeedGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "AI Master Diagnostic Report",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scan.aiSummary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Estimated Repair Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(scan.estimatedTotalRepairCost, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoldPro)
                                }

                                Button(
                                    onClick = { viewModel.toggleFreezeFrameModal(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("freeze_frame_button")
                                ) {
                                    Text("Freeze Frame", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // DTC Items
                if (dtcList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Detected Fault Codes (${dtcList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(dtcList) { dtc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDtc(dtc) }
                                .testTag("dtc_code_item_${dtc.code.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, if (dtc.severity == ScanSeverity.CRITICAL) HazardRed else AlertAmber)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = HazardRed.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = dtc.code,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = HazardRed,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = dtc.system,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = dtc.estimatedRepairCost,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPro
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = dtc.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🔧 Fix: ${dtc.recommendedFix}",
                                    fontSize = 12.sp,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.clearEcuCodes() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("clear_dtc_codes_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, tint = AlertAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear ECU Trouble Codes", color = AlertAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 2: Maintenance Schedule & Task Tracker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Maintenance Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Odometer: $currentMileage mi",
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }

                    Button(
                        onClick = { showAddTaskDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Task", fontSize = 11.sp, color = NeonCyan)
                    }
                }
            }

            items(maintenanceTasks) { task ->
                val milesSince = currentMileage - task.lastCompletedMiles
                val isOverdue = milesSince >= task.intervalMiles
                val milesRemaining = task.intervalMiles - milesSince
                val progress = (milesSince.toFloat() / task.intervalMiles).coerceIn(0f, 1f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, if (isOverdue) HazardRed else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = task.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Every ${task.intervalMiles} mi / ${task.intervalMonths} mo • Last at ${task.lastCompletedMiles} mi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isOverdue) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = HazardRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "OVERDUE +${milesSince - task.intervalMiles} mi",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HazardRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SpeedGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Due in $milesRemaining mi",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SpeedGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOverdue) HazardRed else if (progress > 0.8f) AlertAmber else SpeedGreen,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (task.notes.isNotEmpty()) task.notes else "Standard OEM Specification",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { viewModel.markMaintenanceCompleted(task.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, SpeedGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Done", fontSize = 11.sp, color = SpeedGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 3: Vehicle Damage Scores & Wear Index
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vehicle Damage & Wear Index",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "CAN telemetry & accelerometer damage monitoring",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = SpeedGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${damageReport.overallScore}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpeedGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bars for components
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ComponentDamageBar("Powertrain Integrity", damageReport.powertrainScore, SpeedGreen)
                            ComponentDamageBar("Suspension & Bushings", damageReport.suspensionScore, SpeedGreen)
                            ComponentDamageBar("Exhaust & Gaskets", damageReport.exhaustScore, AlertAmber)
                            ComponentDamageBar("Brake Rotors & Calipers", damageReport.brakeScore, SpeedGreen)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Damage-Informed Route Recommendation",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = damageReport.recommendedRouteAdvice,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Room Database Cached Diagnostic History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cached Diagnostic History (Room)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${scanHistory.size} Records",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (scanHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No cached scans yet in Room database.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Run an AI Quick Scan to cache diagnostic records locally.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(scanHistory) { scan ->
                    val dtcList = viewModel.parseDtcCodes(scan.dtcCodesJson)
                    val healthColor = if (scan.healthScore > 85) SpeedGreen else if (scan.healthScore > 60) AlertAmber else HazardRed

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                        color = healthColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${scan.healthScore}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = healthColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = scan.vehicleName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dateFormat.format(Date(scan.timestamp)),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteScanRecord(scan.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Scan Record", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = scan.aiSummary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (dtcList.isEmpty()) "0 Fault Codes" else "${dtcList.size} DTC Codes Detected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dtcList.isEmpty()) SpeedGreen else HazardRed
                                )

                                Button(
                                    onClick = { viewModel.inspectScanRecord(scan) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Inspect Scan", fontSize = 11.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Run Backend Status Footer
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cloud Run Microservice Linked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "https://gps-route-logic-858314305243.us-east4.run.app",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Task Dialog
        if (showAddTaskDialog) {
            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                title = { Text("Add Maintenance Task", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTaskName,
                            onValueChange = { newTaskName = it },
                            label = { Text("Task Name (e.g. Brake Fluid)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newTaskIntervalMiles,
                            onValueChange = { newTaskIntervalMiles = it },
                            label = { Text("Interval (Miles)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newTaskIntervalMonths,
                            onValueChange = { newTaskIntervalMonths = it },
                            label = { Text("Interval (Months)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTaskName.isNotBlank()) {
                                val miles = newTaskIntervalMiles.toIntOrNull() ?: 5000
                                val months = newTaskIntervalMonths.toIntOrNull() ?: 6
                                viewModel.addMaintenanceTask(newTaskName, "custom", miles, months)
                                showAddTaskDialog = false
                                newTaskName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Add Task", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // DTC Detail Dialog
        if (selectedDtc != null) {
            val dtc = selectedDtc!!
            AlertDialog(
                onDismissRequest = { viewModel.selectDtc(null) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dtc.code, fontWeight = FontWeight.ExtraBold, color = HazardRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = dtc.system, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = dtc.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Symptoms: ${dtc.symptoms}", fontSize = 12.sp)
                        Text(text = "Possible Causes: ${dtc.possibleCauses}", fontSize = 12.sp)
                        Text(text = "Estimated Repair: ${dtc.estimatedRepairCost}", fontWeight = FontWeight.SemiBold, color = GoldPro, fontSize = 13.sp)
                        Text(text = "Recommended Fix: ${dtc.recommendedFix}", fontSize = 12.sp, color = NeonCyan)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.selectDtc(null) }) {
                        Text("Close", color = NeonCyan)
                    }
                }
            )
        }

        // Freeze Frame Dialog
        if (showFreezeFrame) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleFreezeFrameModal(false) },
                title = {
                    Text("ECU Freeze Frame Snapshot", fontWeight = FontWeight.Bold, color = NeonCyan)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Captured at DTC Fault Trigger:", fontSize = 12.sp, color = Color.LightGray)
                        Text("• Engine Speed: ${telemetry.rpm} RPM", fontSize = 12.sp)
                        Text("• Vehicle Speed: ${telemetry.speedMph} MPH", fontSize = 12.sp)
                        Text("• Engine Coolant: ${telemetry.coolantTempF}°F", fontSize = 12.sp)
                        Text("• Calculated Load: ${telemetry.engineLoadPercent}%", fontSize = 12.sp)
                        Text("• Fuel Rail Pressure: ${telemetry.fuelRailPressurePsi} PSI", fontSize = 12.sp)
                        Text("• Throttle Position: ${telemetry.throttlePercent}%", fontSize = 12.sp)
                        Text("• Intake Air Temp: ${telemetry.intakeAirTempF}°F", fontSize = 12.sp)
                        Text("• O2 Sensor Voltage: ${telemetry.o2Sensor1Volt} V", fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleFreezeFrameModal(false) }) {
                        Text("Done", color = NeonCyan)
                    }
                }
            )
        }
    }
}

@Composable
fun ComponentDamageBar(name: String, score: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$score%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.DarkGray
        )
    }
}
