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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.OpenInBrowser
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.BillingPeriod
import com.example.data.model.SubscriptionTier
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GoldPro
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpeedGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AccountScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userAccount.collectAsState()
    val billingPeriod by viewModel.selectedBillingPeriod.collectAsState()
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val authStatus by viewModel.authStatusMessage.collectAsState()
    val showAdDialog by viewModel.showAdRewardDialog.collectAsState()
    val adProgress by viewModel.adWatchProgress.collectAsState()
    val context = LocalContext.current

    var showPrivacyModal by remember { mutableStateOf(false) }
    var showAdsModal by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSuccessModal by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Title
                Text(
                    text = "Account & Subscription",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )
                Text(
                    text = "Google Sign-In, 3-Tier Monetization & Quotas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Google Mobile Authentication Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, if (!user.isAnonymous) SpeedGreen else NeonCyan.copy(alpha = 0.5f))
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
                                    color = if (!user.isAnonymous) SpeedGreen.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (!user.isAnonymous) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = if (!user.isAnonymous) SpeedGreen else NeonCyan,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (!user.isAnonymous && user.email != null) user.email!! else "Guest Mobile Driver",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (user.tier) {
                                    SubscriptionTier.PRO -> GoldPro.copy(alpha = 0.2f)
                                    SubscriptionTier.PREMIUM -> ElectricBlue.copy(alpha = 0.2f)
                                    SubscriptionTier.FREE -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = user.tier.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (user.tier) {
                                        SubscriptionTier.PRO -> GoldPro
                                        SubscriptionTier.PREMIUM -> ElectricBlue
                                        SubscriptionTier.FREE -> NeonCyan
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (authStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = authStatus!!,
                                fontSize = 11.sp,
                                color = SpeedGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Sign-In Actions
                        if (user.isAnonymous) {
                            Button(
                                onClick = { viewModel.signInWithGoogle() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("google_sign_in_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                enabled = !isSigningIn
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Credential Manager Verified", fontSize = 11.sp, color = SpeedGreen)
                                }

                                TextButton(
                                    onClick = { viewModel.signOut() },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Text("Sign Out", color = HazardRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Mobile SHA-1 Fingerprint & Play Console Release Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2E)),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Console & OAuth Configuration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Package: com.aistudio.gpsroutelogic.uid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "SHA-1: 17:15:91:21:4B:29:E7:2C:5B:2C:1C:A3:64:81:8C:99:0D:EA:C2:4B",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Version: ${BuildConfig.VERSION_NAME} (Code: ${BuildConfig.VERSION_CODE}) • Automated Gradle Pipeline",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Cloud Firestore Project & Sync Status Card
            item {
                val syncState by viewModel.firestoreSyncState.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("firestore_sync_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1C33)),
                    border = BorderStroke(1.dp, SpeedGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SpeedGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cloud Firestore Backend", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SpeedGreen)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SpeedGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SpeedGreen)
                            ) {
                                Text(
                                    text = "PROJECT: gps-route-logic",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpeedGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncState.message,
                            fontSize = 11.sp,
                            color = Color(0xFFB0C4DE),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Persistent Collections: /scans, /vehicles, /routes, /chat",
                                fontSize = 10.sp,
                                color = Color(0xFF8D99AE)
                            )

                            Button(
                                onClick = { viewModel.syncWithFirestore() },
                                colors = ButtonDefaults.buttonColors(containerColor = SpeedGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("sync_firestore_button")
                            ) {
                                if (syncState.isSyncing) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Sync to Cloud", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Subscription 3-Tier Plans Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monetization & Plans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Monthly vs Yearly Switcher
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (billingPeriod == BillingPeriod.MONTHLY) NeonCyan else Color.Transparent,
                                modifier = Modifier.clickable { viewModel.setBillingPeriod(BillingPeriod.MONTHLY) }
                            ) {
                                Text(
                                    text = "Monthly",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (billingPeriod == BillingPeriod.MONTHLY) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (billingPeriod == BillingPeriod.YEARLY) NeonCyan else Color.Transparent,
                                modifier = Modifier.clickable { viewModel.setBillingPeriod(BillingPeriod.YEARLY) }
                            ) {
                                Text(
                                    text = "Yearly (Save 20%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (billingPeriod == BillingPeriod.YEARLY) Color.Black else GoldPro,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TIER 1: FREE (Ad-supported)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        width = if (user.tier == SubscriptionTier.FREE) 2.dp else 1.dp,
                        color = if (user.tier == SubscriptionTier.FREE) NeonCyan else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("FREE (Ad-Supported)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("Basic daily diagnostics & GPS routing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$0.00", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NeonCyan)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• 3 AI Diagnostic Scans / Data-sets a day", fontSize = 12.sp)
                        Text("• Standard GPS Navigation & Traffic Alerts", fontSize = 12.sp)
                        Text("• 5 Gemini Real-Time Live-Chat sessions daily", fontSize = 12.sp)
                        Text("• Banner & Rewarded Video Ads included", fontSize = 12.sp, color = AlertAmber)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (user.tier == SubscriptionTier.FREE) {
                                Button(
                                    onClick = { viewModel.showAdReward() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263859)),
                                    border = BorderStroke(1.dp, GoldPro),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("watch_reward_ad_button")
                                ) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = GoldPro, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watch Ad (+1 Scan)", color = GoldPro, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.upgradeSubscription(SubscriptionTier.FREE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.tier == SubscriptionTier.FREE) MaterialTheme.colorScheme.outline else NeonCyan
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (user.tier == SubscriptionTier.FREE) "Current Plan" else "Select Free",
                                    color = if (user.tier == SubscriptionTier.FREE) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // TIER 2: PREMIUM ($4.99/mo or $49.99/yr)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        width = if (user.tier == SubscriptionTier.PREMIUM) 2.dp else 1.dp,
                        color = if (user.tier == SubscriptionTier.PREMIUM) ElectricBlue else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PREMIUM", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ElectricBlue)
                                Text("For active daily drivers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (billingPeriod == BillingPeriod.MONTHLY) "$4.99 / mo" else "$49.99 / yr",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = ElectricBlue
                                )
                                if (billingPeriod == BillingPeriod.YEARLY) {
                                    Text("Save 17% vs monthly", fontSize = 10.sp, color = SpeedGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• 15 AI Diagnostic Scans / Data-sets a day", fontSize = 12.sp)
                        Text("• 35 Real-Time Gemini Live-Chat Sessions / day", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        Text("• 100% Ad-Free Experience", fontSize = 12.sp, color = SpeedGreen)
                        Text("• Advanced Personalized Routes (Fastest, Eco, Scenic, Heavy)", fontSize = 12.sp)
                        Text("• Real-Time Lane Assist & Incident Bypass", fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.upgradeSubscription(SubscriptionTier.PREMIUM) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("upgrade_premium_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.tier == SubscriptionTier.PREMIUM) MaterialTheme.colorScheme.outline else ElectricBlue
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (user.tier == SubscriptionTier.PREMIUM) "Active Plan" else "Upgrade to Premium",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // TIER 3: PRO ($8.99/mo or $84.99/yr)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B233D)),
                    border = BorderStroke(2.dp, GoldPro)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldPro, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("PRO MAX", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = GoldPro)
                                    Text("Maximum features included", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (billingPeriod == BillingPeriod.MONTHLY) "$8.99 / mo" else "$84.99 / yr",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = GoldPro
                                )
                                if (billingPeriod == BillingPeriod.YEARLY) {
                                    Text("Save 21% vs monthly", fontSize = 10.sp, color = GoldPro, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• UNLIMITED Real-Time Gemini Live-Chat & Voice Co-Pilot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPro)
                        Text("• 50 AI Diagnostic Scans / Data-sets a day", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("• Deep OBD-II Freeze Frame ECU Sensor Inspection", fontSize = 12.sp)
                        Text("• Multi-Stop Optimized Personalized Route Engine", fontSize = 12.sp)
                        Text("• Predictive Maintenance Timeline & DTC Clearing Guides", fontSize = 12.sp)
                        Text("• Export PDF/CSV Master Diagnostic Reports", fontSize = 12.sp)
                        Text("• 100% Ad-Free & Priority Cloud Sync", fontSize = 12.sp, color = SpeedGreen)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.upgradeSubscription(SubscriptionTier.PRO) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("upgrade_pro_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.tier == SubscriptionTier.PRO) MaterialTheme.colorScheme.outline else GoldPro
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (user.tier == SubscriptionTier.PRO) "Active PRO Max Plan" else "Upgrade to PRO Max",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Section: Legal, Privacy & Data Safety Hub
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Legal, Privacy & Data Control",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Google Play Data Safety & IAB Tech Lab compliance verified for package com.aistudio.gpsroutelogic.uid",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row of Legal Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPrivacyModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16233B)),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("view_privacy_policy_button")
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Privacy Policy", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = { showAdsModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16233B)),
                                border = BorderStroke(1.dp, GoldPro.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("view_ad_policy_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GoldPro, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ad Disclosure", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Data Deletion Request Button
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HazardRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, HazardRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("request_data_deletion_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = HazardRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Data Deletion & Wipe Account", color = HazardRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Privacy Policy Modal Dialog
        if (showPrivacyModal) {
            AlertDialog(
                onDismissRequest = { showPrivacyModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeonCyan)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GPS-Route-Logic (com.aistudio.gpsroutelogic.uid)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "• Location & GPS: Processed in real-time during active trips to calculate multi-profile routes (Fastest, Eco, Scenic, Cargo). We do not sell live location data.\n\n" +
                                    "• OBD-II Diagnostics: Stored locally in encrypted on-device SQLite database and synchronized only when logged in.\n\n" +
                                    "• Google Credential Manager: Native Android sign-in. We never access your Google password.\n\n" +
                                    "• Voice & Gemini AI: Voice co-pilot prompts are transcribed on-device; audio files are not permanently stored on remote servers.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-pre-7wsizo553tjveiu2qj2qmb-333894732567.us-west2.run.app/privacy-policy.html"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                            showPrivacyModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Full Policy URL", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrivacyModal = false }) {
                        Text("Close", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            )
        }

        // Advertisement & app-ads.txt Modal Dialog
        if (showAdsModal) {
            AlertDialog(
                onDismissRequest = { showAdsModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldPro)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ad Disclosure & app-ads.txt", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GoldPro)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "IAB Tech Lab & Google AdMob Disclosure",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "• Publisher ID: pub-2800841113603313\n" +
                                    "• app-ads.txt: google.com, pub-2800841113603313, DIRECT, f08c47fec0942fa0\n\n" +
                                    "• Free Tier: Includes non-intrusive banners & optional rewarded video ads.\n" +
                                    "• Paid Tiers (Premium & PRO Max): 100% ad-free experience with all ad network requests disabled.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-pre-7wsizo553tjveiu2qj2qmb-333894732567.us-west2.run.app/app-ads.txt"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                            showAdsModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPro)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify app-ads.txt URL", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdsModal = false }) {
                        Text("Close", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            )
        }

        // Data Deletion Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = HazardRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Data Deletion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HazardRed)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Are you sure you want to permanently erase your account data?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "This will immediately:\n" +
                                    "1. Wipe all local & cloud OBD-II diagnostic scans.\n" +
                                    "2. Clear all saved favorite routes and garage vehicles.\n" +
                                    "3. Sign out of Google Credential Manager and reset app to factory defaults.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllUserDataAndReset {
                                showDeleteConfirmDialog = false
                                showDeleteSuccessModal = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HazardRed),
                        modifier = Modifier.testTag("confirm_delete_data_button")
                    ) {
                        Text("Erase All Data Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            )
        }

        // Deletion Success Modal
        if (showDeleteSuccessModal) {
            AlertDialog(
                onDismissRequest = { showDeleteSuccessModal = false },
                title = { Text("Account Data Purged", fontWeight = FontWeight.Bold, color = SpeedGreen, fontSize = 16.sp) },
                text = {
                    Text(
                        text = "All your diagnostic scan logs, saved routes, and vehicle profiles have been permanently deleted from the device database and your session has been reset.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showDeleteSuccessModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedGreen)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Ad Simulation Dialog
        if (showAdDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAdReward() },
                title = { Text("Sponsored Ad Break", fontWeight = FontWeight.Bold, color = GoldPro) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Simulating 3-second sponsor ad for +1 Free AI Scan...", fontSize = 12.sp)
                        CircularProgressIndicator(
                            progress = { adProgress },
                            modifier = Modifier.size(48.dp),
                            color = GoldPro
                        )
                        Text("Sponsor: Bosch Automotive Diagnostics", fontSize = 11.sp, color = Color.LightGray)
                    }
                },
                confirmButton = {}
            )
        }
    }
}
