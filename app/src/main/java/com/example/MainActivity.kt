package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.data.model.BillingPeriod
import com.example.data.model.SubscriptionTier
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.RouteScreen
import com.example.ui.screens.VoiceCopilotScreen
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GPSRouteLogicTheme
import com.example.ui.theme.GoldPro
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSRouteLogicTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val showPaywall by viewModel.showPaywallModal.collectAsState()
    val billingPeriod by viewModel.selectedBillingPeriod.collectAsState()
    val user by viewModel.userAccount.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Audio permission granted for speech recognition and live voice copilot
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(SpeedGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GPS ROUTE LOGIC",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "AI Telemetry & Diagnostics",
                                fontSize = 10.sp,
                                color = Color(0xFF8D99AE)
                            )
                        }
                    }
                },
                actions = {
                    // Google Sign-In / Account Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (!user.isAnonymous) SpeedGreen.copy(alpha = 0.15f) else Color(0xFF1C2B54),
                        border = BorderStroke(1.dp, if (!user.isAnonymous) SpeedGreen else NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                if (user.isAnonymous) {
                                    viewModel.signInWithGoogle()
                                } else {
                                    viewModel.selectTab(AppTab.ACCOUNT)
                                }
                            }
                            .testTag("topbar_google_auth_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (!user.isAnonymous) Icons.Default.VerifiedUser else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (!user.isAnonymous) SpeedGreen else NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (!user.isAnonymous) (user.displayName.take(12)) else "Google Sign-In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!user.isAnonymous) SpeedGreen else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B132B),
                    titleContentColor = NeonCyan
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B132B),
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.ROUTE,
                    onClick = { viewModel.selectTab(AppTab.ROUTE) },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = "Route") },
                    label = { Text("Route", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_tab_route")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.DIAGNOSTICS,
                    onClick = { viewModel.selectTab(AppTab.DIAGNOSTICS) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Diagnostics") },
                    label = { Text("Diagnostics", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_tab_diagnostics")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.VOICE,
                    onClick = { viewModel.selectTab(AppTab.VOICE) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Voice") },
                    label = { Text("Co-Pilot", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_tab_voice")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.GARAGE,
                    onClick = { viewModel.selectTab(AppTab.GARAGE) },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Garage") },
                    label = { Text("Garage", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_tab_garage")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ACCOUNT,
                    onClick = { viewModel.selectTab(AppTab.ACCOUNT) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Account") },
                    label = { Text("Account", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_tab_account")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Cockpit Holographic Backdrop
            Image(
                painter = painterResource(id = R.drawable.drive_logic_ai_developer_header),
                contentDescription = "DriveLogic AI Cockpit Backdrop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Cyberpunk dark gradient scrim to ensure complete contrast and legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF070B19).copy(alpha = 0.86f),
                                Color(0xFF0A1128).copy(alpha = 0.93f)
                            )
                        )
                    )
            )

            when (currentTab) {
                AppTab.ROUTE -> RouteScreen(viewModel = viewModel)
                AppTab.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                AppTab.VOICE -> VoiceCopilotScreen(viewModel = viewModel)
                AppTab.GARAGE -> GarageScreen(viewModel = viewModel)
                AppTab.ACCOUNT -> AccountScreen(viewModel = viewModel)
            }
        }

        // Paywall Upgrade Modal Dialog
        if (showPaywall) {
            AlertDialog(
                onDismissRequest = { viewModel.showPaywall(false) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldPro)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upgrade Your Daily Limits", fontWeight = FontWeight.Bold, color = GoldPro)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "You have reached your daily quota on the Free plan. Upgrade to unlock more diagnostic scans and live voice co-pilot tokens!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Premium Option
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, ElectricBlue)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Premium Plan", fontWeight = FontWeight.Bold, color = ElectricBlue)
                                    Text(
                                        if (billingPeriod == BillingPeriod.MONTHLY) "$4.99/mo" else "$49.99/yr",
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                }
                                Text("• 15 Scans/day + 35 Live-Chat Sessions + Ad-Free", fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.upgradeSubscription(SubscriptionTier.PREMIUM) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Choose Premium", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PRO Option
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B233D)),
                            border = BorderStroke(1.5.dp, GoldPro)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PRO Max Plan", fontWeight = FontWeight.ExtraBold, color = GoldPro)
                                    Text(
                                        if (billingPeriod == BillingPeriod.MONTHLY) "$8.99/mo" else "$84.99/yr",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPro
                                    )
                                }
                                Text("• 50 Scans/day + Unlimited Real-Time Chat + Full ECU Features", fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.upgradeSubscription(SubscriptionTier.PRO) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPro),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Choose PRO Max", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.showPaywall(false) }) {
                        Text("Maybe Later", color = Color.LightGray)
                    }
                }
            )
        }
    }
}
