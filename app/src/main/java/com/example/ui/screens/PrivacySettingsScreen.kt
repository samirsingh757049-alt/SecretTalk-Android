package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardBg
import com.example.ui.theme.IosDivider
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurface
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHiddenIcon: () -> Unit,
    onLockNow: () -> Unit,
    onLogout: () -> Unit,
    onChangeSecretCode: (newCode: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var showChangeCodeDialog by remember { mutableStateOf(false) }
    var newCodeInput by remember { mutableStateOf("") }
    var confirmCodeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var changeSuccessMsg by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = IosBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Privacy & Security",
                        color = IosTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
            if (changeSuccessMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IosGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = changeSuccessMsg!!,
                        color = IosGreen,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Section 1: Security Controls
            Text(
                text = "ACCESS & LOCK",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IosTextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.LockReset,
                        iconTint = IosOrange,
                        title = "Change Secret Access Code",
                        subtitle = "Update your custom 4-8 digit passcode",
                        onClick = { showChangeCodeDialog = true }
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(IosDivider))

                    SettingsRow(
                        icon = Icons.Default.Lock,
                        iconTint = IosBlue,
                        title = "Lock Session Now",
                        subtitle = "Immediately require Secret Code to unlock",
                        onClick = onLockNow
                    )
                }
            }

            // Section 2: Stealth
            Text(
                text = "STEALTH SETTINGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IosTextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
            ) {
                SettingsRow(
                    icon = Icons.Default.VisibilityOff,
                    iconTint = IosTextPrimary,
                    title = "Hidden Launcher Icon",
                    subtitle = "Hide or restore app from home screen",
                    onClick = onNavigateToHiddenIcon
                )
            }

            // Section 3: Cryptography Audit Info
            Text(
                text = "SECURITY ARCHITECTURE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IosTextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Two-Account Limit", color = IosTextSecondary, fontSize = 14.sp)
                        Text("Strictly 2 (Enforced)", color = IosGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Message Encryption", color = IosTextSecondary, fontSize = 14.sp)
                        Text("AES-256-GCM", color = IosTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Key Derivation", color = IosTextSecondary, fontSize = 14.sp)
                        Text("PBKDF2-HMAC-SHA256", color = IosTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Call Signaling", color = IosTextSecondary, fontSize = 14.sp)
                        Text("WebRTC P2P (Google STUN)", color = IosTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            // Sign Out
            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
            ) {
                SettingsRow(
                    icon = Icons.Default.PowerSettingsNew,
                    iconTint = IosRed,
                    title = "Sign Out & Terminate Session",
                    subtitle = "Clears session key and logs out",
                    onClick = onLogout
                )
            }
        }
    }

    if (showChangeCodeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeCodeDialog = false },
            title = { Text("Change Secret Code", color = IosTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCodeInput,
                        onValueChange = { if (it.length <= 8) newCodeInput = it },
                        label = { Text("New Secret Code (4-8 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = IosTextPrimary,
                            unfocusedTextColor = IosTextPrimary,
                            focusedBorderColor = IosBlue,
                            unfocusedBorderColor = IosDivider
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmCodeInput,
                        onValueChange = { if (it.length <= 8) confirmCodeInput = it },
                        label = { Text("Confirm New Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = IosTextPrimary,
                            unfocusedTextColor = IosTextPrimary,
                            focusedBorderColor = IosBlue,
                            unfocusedBorderColor = IosDivider
                        ),
                        singleLine = true
                    )

                    if (codeError != null) {
                        Text(codeError!!, color = IosRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCodeInput.length < 4) {
                            codeError = "Code must be at least 4 digits"
                            return@TextButton
                        }
                        if (newCodeInput != confirmCodeInput) {
                            codeError = "Codes do not match"
                            return@TextButton
                        }
                        codeError = null
                        onChangeSecretCode(newCodeInput) { success ->
                            if (success) {
                                showChangeCodeDialog = false
                                newCodeInput = ""
                                confirmCodeInput = ""
                                changeSuccessMsg = "Secret Access Code updated successfully"
                            } else {
                                codeError = "Failed to update Secret Code"
                            }
                        }
                    }
                ) {
                    Text("Update", color = IosBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeCodeDialog = false }) {
                    Text("Cancel", color = IosTextSecondary)
                }
            },
            containerColor = IosCardBg
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = IosTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = IosTextSecondary
                )
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = IosTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
