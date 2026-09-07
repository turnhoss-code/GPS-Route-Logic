package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleProfile
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GarageScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val isSpeaking by viewModel.isVoiceSpeaking.collectAsState()

    val activeVehicle = vehicles.firstOrNull { it.isDefault } ?: vehicles.firstOrNull()

    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<VehicleProfile?>(null) }

    // Dialog state for adding
    var inputNickname by remember { mutableStateOf("") }
    var inputYear by remember { mutableStateOf("") }
    var inputMake by remember { mutableStateOf("") }
    var inputModel by remember { mutableStateOf("") }
    var inputTrim by remember { mutableStateOf("") }
    var inputEngine by remember { mutableStateOf("") }
    var inputSize by remember { mutableStateOf("") }
    var inputTransmission by remember { mutableStateOf("") }
    var inputDriveType by remember { mutableStateOf("") }
    var inputHorsepower by remember { mutableStateOf("") }
    var inputFuelType by remember { mutableStateOf("") }
    var inputTireSpec by remember { mutableStateOf("") }
    var inputMileage by remember { mutableStateOf("") }
    var inputVin by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Title and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Vehicle Garage",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                        Text(
                            text = "Hardware Telemetry & Deep Vehicle Specs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.testTag("add_vehicle_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Car", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // British Accent Natural Language Greeting Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14213D)),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NeonCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🇬🇧", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Route Logic Voice Co-Pilot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "\"Im route logic. how can i help you?\"",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.greetUserOnOpen(force = true) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) SpeedGreen else NeonCyan.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.testTag("replay_greeting_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play Greeting",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSpeaking) "Speaking..." else "Play",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Active Vehicle Spec Boxes Section
            if (activeVehicle != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Vehicle Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeVehicle.year} ${activeVehicle.make} ${activeVehicle.model} • ${activeVehicle.trim}",
                                fontSize = 12.sp,
                                color = NeonCyan
                            )
                        }

                        IconButton(
                            onClick = { vehicleToEdit = activeVehicle }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Specs", tint = NeonCyan)
                        }
                    }
                }

                // 1. Engine Details Box
                item {
                    VehicleDetailBox(
                        title = "Engine & Powertrain",
                        icon = Icons.Default.Build,
                        iconTint = AlertAmber,
                        headline = activeVehicle.engine,
                        subHeadline = "${activeVehicle.horsepower} • ${activeVehicle.torque}",
                        tags = listOf(
                            "Valvetrain" to "DOHC 16-Valve VTEC",
                            "Displacement" to "1,996 cc Turbocharged",
                            "Fuel Delivery" to "Direct Fuel Injection",
                            "Oil Capacity" to activeVehicle.oilSpec
                        )
                    )
                }

                // 2. Vehicle Size & Dimensions Box
                item {
                    VehicleDetailBox(
                        title = "Vehicle Size & Classification",
                        icon = Icons.Default.Straighten,
                        iconTint = NeonCyan,
                        headline = activeVehicle.size,
                        subHeadline = "${activeVehicle.bodyType} • Curb: ${activeVehicle.curbWeight}",
                        tags = listOf(
                            "Size Class" to "Mid-Size EPA Class",
                            "Curb Weight" to activeVehicle.curbWeight,
                            "Towing Max" to activeVehicle.towingCapacity,
                            "Seating" to "5-Passenger Capacity"
                        )
                    )
                }

                // 3. Trim & Model Edition Box
                item {
                    VehicleDetailBox(
                        title = "Trim Package & Equipment",
                        icon = Icons.Default.Star,
                        iconTint = GoldPro,
                        headline = activeVehicle.trim,
                        subHeadline = "VIN: ${activeVehicle.vin}",
                        tags = listOf(
                            "Trim Package" to activeVehicle.trim,
                            "Lighting" to "Full LED Projector Matrix",
                            "Audio & HUD" to "12-Speaker Premium Bose",
                            "Safety Suite" to "Radar ACC & Lane Keep"
                        )
                    )
                }

                // 4. Transmission & Drivetrain Box
                item {
                    VehicleDetailBox(
                        title = "Transmission & Drivetrain",
                        icon = Icons.Default.Engineering,
                        iconTint = SpeedGreen,
                        headline = activeVehicle.transmission,
                        subHeadline = activeVehicle.driveType,
                        tags = listOf(
                            "Drivetrain" to activeVehicle.driveType,
                            "Gearbox" to activeVehicle.transmission,
                            "Drive Modes" to "Eco, Normal, Sport, Snow",
                            "Diff Lock" to "Electronic Torque Vectoring"
                        )
                    )
                }

                // 5. Fuel, Tank & Range Box
                item {
                    VehicleDetailBox(
                        title = "Fuel & Fluid Specifications",
                        icon = Icons.Default.LocalGasStation,
                        iconTint = AlertAmber,
                        headline = activeVehicle.fuelType,
                        subHeadline = "Fuel Tank: ${activeVehicle.tankCapacity}",
                        tags = listOf(
                            "Fuel Grade" to "91+ Octane Recommended",
                            "Tank Capacity" to activeVehicle.tankCapacity,
                            "Engine Coolant" to activeVehicle.coolantSpec,
                            "Brake Fluid" to activeVehicle.brakeFluidSpec
                        )
                    )
                }

                // 6. Tires & Chassis Box
                item {
                    VehicleDetailBox(
                        title = "Tires, Wheels & Cold PSI",
                        icon = Icons.Default.PanoramaHorizontal,
                        iconTint = NeonCyan,
                        headline = activeVehicle.tireSpec,
                        subHeadline = "Alloy Wheels: 19\" Machine-Finished Gloss Black",
                        tags = listOf(
                            "Tire Spec" to activeVehicle.tireSpec,
                            "Front Pressure" to "33 PSI Cold",
                            "Rear Pressure" to "32 PSI Cold",
                            "TPMS Sensor" to "Direct OBD-II 433MHz"
                        )
                    )
                }

                // 7. OBD-II Hardware & ECU Calibration Box
                item {
                    VehicleDetailBox(
                        title = "OBD-II Bus & ECU Calibration",
                        icon = Icons.Default.Memory,
                        iconTint = ElectricBlue,
                        headline = activeVehicle.obdProtocol,
                        subHeadline = "ECU Calibration: ${activeVehicle.ecuFirmware}",
                        tags = listOf(
                            "Bus Protocol" to activeVehicle.obdProtocol,
                            "ECU Firmware" to activeVehicle.ecuFirmware,
                            "Baud Rate" to "500 kbaud (High-Speed)",
                            "Pinout Spec" to "SAE J1962 DLC 16-Pin"
                        )
                    )
                }
            }

            // OBD-II Scanner Hardware Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, SpeedGreen.copy(alpha = 0.5f))
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
                                    color = SpeedGreen.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("OBDLink MX+ Bluetooth", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Status: Active Telemetry Paired", fontSize = 11.sp, color = SpeedGreen)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpeedGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "CAN 500kbps",
                                    color = SpeedGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Supported Protocols: ISO 15765-4 (CAN), SAE J1850 PWM/VPW, ISO 9141-2 K-Line, ISO 14230-4 KWP.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // All Saved Vehicles List
            item {
                Text(
                    text = "Saved Vehicles (${vehicles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(vehicles) { vehicle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDefaultVehicle(vehicle.id) }
                        .testTag("vehicle_card_${vehicle.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (vehicle.isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (vehicle.isDefault) 2.dp else 1.dp,
                        color = if (vehicle.isDefault) NeonCyan else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = vehicle.trim,
                                        fontSize = 11.sp,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (vehicle.isDefault) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Active Vehicle",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { vehicleToEdit = vehicle },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("ENGINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertAmber)
                                    Text(vehicle.engine, fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("SIZE / CLASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    Text(vehicle.size, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "VIN: ${if (vehicle.vin.isNotBlank()) vehicle.vin else "1HGCV1F34PA092811"} • ${vehicle.mileage} miles",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Past Diagnostic Scan History
            item {
                Text(
                    text = "Diagnostic Scan History (${scanHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (scanHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No past diagnostic records found. Run an AI Quick Scan to log reports.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(scanHistory) { scan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(scan.vehicleName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Score: ${scan.healthScore}/100 • ${scan.severity}", fontSize = 11.sp, color = NeonCyan)
                            }
                            Text(scan.estimatedTotalRepairCost, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPro)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Vehicle Dialog with full detail boxes fields
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    inputNickname = ""
                    inputYear = ""
                    inputMake = ""
                    inputModel = ""
                    inputTrim = ""
                    inputEngine = ""
                    inputSize = ""
                    inputTransmission = ""
                    inputDriveType = ""
                    inputHorsepower = ""
                    inputFuelType = ""
                    inputTireSpec = ""
                    inputMileage = ""
                    inputVin = ""
                },
                title = { Text("Add New Vehicle Profile", fontWeight = FontWeight.Bold, color = NeonCyan) },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = inputNickname,
                                onValueChange = { inputNickname = it },
                                label = { Text("Nickname") },
                                placeholder = { Text("e.g. Daily Driver") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = inputYear,
                                    onValueChange = { inputYear = it },
                                    label = { Text("Year") },
                                    placeholder = { Text("e.g. 2024") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = inputMake,
                                    onValueChange = { inputMake = it },
                                    label = { Text("Make") },
                                    placeholder = { Text("e.g. Toyota, Ford") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = inputModel,
                                onValueChange = { inputModel = it },
                                label = { Text("Model") },
                                placeholder = { Text("e.g. Camry, F-150") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputTrim,
                                onValueChange = { inputTrim = it },
                                label = { Text("Trim Level") },
                                placeholder = { Text("e.g. Touring, SE, Limited") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputEngine,
                                onValueChange = { inputEngine = it },
                                label = { Text("Engine Specs") },
                                placeholder = { Text("e.g. 2.5L 4-Cyl, 3.5L V6") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputSize,
                                onValueChange = { inputSize = it },
                                label = { Text("Vehicle Class") },
                                placeholder = { Text("e.g. Mid-Size Sedan, Compact SUV") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputHorsepower,
                                onValueChange = { inputHorsepower = it },
                                label = { Text("Horsepower / Torque") },
                                placeholder = { Text("e.g. 203 hp") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputTransmission,
                                onValueChange = { inputTransmission = it },
                                label = { Text("Transmission") },
                                placeholder = { Text("e.g. 8-Speed Automatic, CVT") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputDriveType,
                                onValueChange = { inputDriveType = it },
                                label = { Text("Drivetrain") },
                                placeholder = { Text("e.g. FWD, AWD, 4WD") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputFuelType,
                                onValueChange = { inputFuelType = it },
                                label = { Text("Fuel Grade") },
                                placeholder = { Text("e.g. Regular Unleaded, Premium") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputTireSpec,
                                onValueChange = { inputTireSpec = it },
                                label = { Text("Tire Spec") },
                                placeholder = { Text("e.g. 215/55R17 32 PSI") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputMileage,
                                onValueChange = { inputMileage = it },
                                label = { Text("Current Mileage") },
                                placeholder = { Text("e.g. 15000") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = inputVin,
                                onValueChange = { inputVin = it },
                                label = { Text("VIN (17-character)") },
                                placeholder = { Text("Optional 17-character VIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val curYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                            val y = inputYear.trim()
                            val m = inputMake.trim()
                            val mo = inputModel.trim()
                            val fallbackName = listOf(y, m, mo).filter { it.isNotEmpty() }.joinToString(" ")
                            viewModel.addVehicle(
                                nickname = inputNickname.trim().ifBlank { fallbackName.ifBlank { "Vehicle Profile" } },
                                year = y.toIntOrNull() ?: curYear,
                                make = m.ifBlank { "Vehicle" },
                                model = mo.ifBlank { "Standard" },
                                trim = inputTrim.trim().ifBlank { "Standard" },
                                engine = inputEngine.trim().ifBlank { "Standard Engine" },
                                size = inputSize.trim().ifBlank { "Mid-Size" },
                                transmission = inputTransmission.trim().ifBlank { "Automatic" },
                                driveType = inputDriveType.trim().ifBlank { "FWD" },
                                horsepower = inputHorsepower.trim().ifBlank { "N/A" },
                                fuelType = inputFuelType.trim().ifBlank { "Regular Unleaded" },
                                tireSpec = inputTireSpec.trim().ifBlank { "Standard" },
                                mileage = inputMileage.trim().toIntOrNull() ?: 0,
                                vin = inputVin.trim()
                            )
                            inputNickname = ""
                            inputYear = ""
                            inputMake = ""
                            inputModel = ""
                            inputTrim = ""
                            inputEngine = ""
                            inputSize = ""
                            inputTransmission = ""
                            inputDriveType = ""
                            inputHorsepower = ""
                            inputFuelType = ""
                            inputTireSpec = ""
                            inputMileage = ""
                            inputVin = ""
                            showAddDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save Car", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        inputNickname = ""
                        inputYear = ""
                        inputMake = ""
                        inputModel = ""
                        inputTrim = ""
                        inputEngine = ""
                        inputSize = ""
                        inputTransmission = ""
                        inputDriveType = ""
                        inputHorsepower = ""
                        inputFuelType = ""
                        inputTireSpec = ""
                        inputMileage = ""
                        inputVin = ""
                    }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                }
            )
        }

        // Edit Vehicle Dialog
        vehicleToEdit?.let { currentProfile ->
            var editNickname by remember(currentProfile.id) { mutableStateOf(currentProfile.nickname) }
            var editMake by remember(currentProfile.id) { mutableStateOf(currentProfile.make) }
            var editModel by remember(currentProfile.id) { mutableStateOf(currentProfile.model) }
            var editTrim by remember(currentProfile.id) { mutableStateOf(currentProfile.trim) }
            var editEngine by remember(currentProfile.id) { mutableStateOf(currentProfile.engine) }
            var editSize by remember(currentProfile.id) { mutableStateOf(currentProfile.size) }
            var editTransmission by remember(currentProfile.id) { mutableStateOf(currentProfile.transmission) }
            var editHorsepower by remember(currentProfile.id) { mutableStateOf(currentProfile.horsepower) }
            var editFuelType by remember(currentProfile.id) { mutableStateOf(currentProfile.fuelType) }
            var editTireSpec by remember(currentProfile.id) { mutableStateOf(currentProfile.tireSpec) }
            var editVin by remember(currentProfile.id) { mutableStateOf(currentProfile.vin) }

            AlertDialog(
                onDismissRequest = { vehicleToEdit = null },
                title = { Text("Edit Vehicle Specifications", fontWeight = FontWeight.Bold, color = NeonCyan) },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = editTrim,
                                onValueChange = { editTrim = it },
                                label = { Text("Trim Package") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editEngine,
                                onValueChange = { editEngine = it },
                                label = { Text("Engine Details") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editSize,
                                onValueChange = { editSize = it },
                                label = { Text("Vehicle Size & Dimensions") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editHorsepower,
                                onValueChange = { editHorsepower = it },
                                label = { Text("Horsepower & Torque") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editTransmission,
                                onValueChange = { editTransmission = it },
                                label = { Text("Transmission & Drive Type") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editFuelType,
                                onValueChange = { editFuelType = it },
                                label = { Text("Fuel Grade & Tank") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editTireSpec,
                                onValueChange = { editTireSpec = it },
                                label = { Text("Tire Specification") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editVin,
                                onValueChange = { editVin = it },
                                label = { Text("VIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateVehicle(
                                currentProfile.copy(
                                    nickname = editNickname,
                                    make = editMake,
                                    model = editModel,
                                    trim = editTrim,
                                    engine = editEngine,
                                    size = editSize,
                                    transmission = editTransmission,
                                    horsepower = editHorsepower,
                                    fuelType = editFuelType,
                                    tireSpec = editTireSpec,
                                    vin = editVin
                                )
                            )
                            vehicleToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Update Specs", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vehicleToEdit = null }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                }
            )
        }
    }
}

@Composable
fun VehicleDetailBox(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    headline: String,
    subHeadline: String,
    tags: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
        border = BorderStroke(1.dp, Color(0xFF243350))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                    Text(
                        text = headline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            if (subHeadline.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subHeadline,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF243350), thickness = 0.75.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Grid of specific metrics
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPair.forEach { (label, value) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (rowPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
