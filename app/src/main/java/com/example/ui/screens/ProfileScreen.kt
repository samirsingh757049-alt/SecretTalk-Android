package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemPairing
import com.example.data.model.UserProfile
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardBg
import com.example.ui.theme.IosDivider
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosSurface
import com.example.ui.theme.IosSurfaceVariant
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    myProfile: UserProfile?,
    partnerProfile: UserProfile?,
    currentPairing: SystemPairing?,
    onNavigateBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = IosBackground,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Pairing", color = IosTextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = IosBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IosSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // My Profile Card
            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(IosBlue.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (myProfile?.displayName?.take(1) ?: "U").uppercase(),
                            color = IosBlue,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = myProfile?.displayName ?: "My Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = IosTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (myProfile?.role == "owner") "Role: User A (Owner)" else "Role: User B (Member)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (myProfile?.role == "owner") IosOrange else IosGreen,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Partner Profile Card
            Text(
                text = "PAIRED PARTNER",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(IosSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (partnerProfile?.displayName?.take(1) ?: "P").uppercase(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partnerProfile?.displayName ?: "Awaiting Partner Pairing",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = IosTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (partnerProfile?.isOnline == true) "Online" else "Offline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (partnerProfile?.isOnline == true) IosGreen else IosTextSecondary
                            )
                        )
                    }

                    if (partnerProfile != null) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Pair",
                            tint = IosGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Invitation Code (for User A)
            if (myProfile?.role == "owner" && currentPairing != null) {
                Text(
                    text = "INVITATION CODE FOR USER B",
                    style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = IosCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = currentPairing.inviteCode,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = IosOrange,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentPairing.isLocked) "Used (Registration locked)" else "Share this with User B to authorize",
                                color = IosTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentPairing.inviteCode))
                                copiedNotice = "Invite code copied to clipboard!"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Invite Code",
                                tint = IosBlue
                            )
                        }
                    }
                }
            }

            // System Pairing Status
            Text(
                text = "SYSTEM REGISTRATION STATUS",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Accounts", color = IosTextSecondary, fontSize = 14.sp)
                        Text(
                            "${currentPairing?.registeredCount ?: 1} of 2 registered",
                            color = IosTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Registration Lock", color = IosTextSecondary, fontSize = 14.sp)
                        Text(
                            if (currentPairing?.isLocked == true) "LOCKED (No further users)" else "OPEN (Awaiting User B)",
                            color = if (currentPairing?.isLocked == true) IosGreen else IosOrange,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (copiedNotice != null) {
                Text(
                    text = copiedNotice!!,
                    color = IosGreen,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
