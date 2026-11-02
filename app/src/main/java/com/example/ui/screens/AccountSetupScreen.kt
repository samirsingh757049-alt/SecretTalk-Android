package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PairingScreenState
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardBg
import com.example.ui.theme.IosDivider
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurfaceVariant
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary

@Composable
fun AccountSetupScreen(
    pairingState: PairingScreenState,
    onRegisterOwner: (displayName: String, secretCode: String) -> Unit,
    onPairMember: (inviteCode: String, displayName: String, secretCode: String) -> Unit,
    onLogout: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var secretCode by remember { mutableStateOf("") }
    var confirmSecretCode by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (pairingState) {
            is PairingScreenState.RegistrationClosed -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = IosCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(IosRed.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Registration Closed",
                                tint = IosRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Registration Closed",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IosTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pairingState.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = IosTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = IosSurfaceVariant)
                        ) {
                            Text("Sign Out", color = IosTextPrimary)
                        }
                    }
                }
            }

            PairingScreenState.UnregisteredOwner -> {
                // User A Setup
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = IosCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(IosBlue.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Owner Account",
                                tint = IosBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Provision User A (Owner)",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IosTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "You are the primary user. Set your credentials to generate User B's invitation code.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IosTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosBlue,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosBlue,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { if (it.length <= 8) secretCode = it },
                            label = { Text("Custom Secret Access Code (4-8 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_secret_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosBlue,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosBlue,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = confirmSecretCode,
                            onValueChange = { if (it.length <= 8) confirmSecretCode = it },
                            label = { Text("Confirm Secret Access Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_confirm_secret_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosBlue,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosBlue,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (localError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = localError!!,
                                color = IosRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (displayName.isBlank()) {
                                    localError = "Please enter your display name"
                                    return@Button
                                }
                                if (secretCode.length < 4) {
                                    localError = "Secret code must be at least 4 digits"
                                    return@Button
                                }
                                if (secretCode != confirmSecretCode) {
                                    localError = "Secret codes do not match"
                                    return@Button
                                }
                                isSubmitting = true
                                localError = null
                                onRegisterOwner(displayName, secretCode)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_owner_setup"),
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Provision Messenger", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = onLogout) {
                            Text("Sign Out", color = IosTextSecondary)
                        }
                    }
                }
            }

            PairingScreenState.AwaitingUserB -> {
                // User B Pairing
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = IosCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(IosGreen.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Member Pairing",
                                tint = IosGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Pair as User B",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IosTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Enter User A's one-time invitation code to permanently pair accounts.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IosTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { if (it.length <= 8) inviteCodeInput = it.uppercase() },
                            label = { Text("6-Character Invitation Code") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosGreen,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosGreen,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Your Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosGreen,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosGreen,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { if (it.length <= 8) secretCode = it },
                            label = { Text("Your Secret Access Code (4-8 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_secret_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosGreen,
                                unfocusedBorderColor = IosDivider,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary,
                                focusedLabelColor = IosGreen,
                                unfocusedLabelColor = IosTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (localError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = localError!!,
                                color = IosRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (inviteCodeInput.isBlank()) {
                                    localError = "Please enter the invitation code"
                                    return@Button
                                }
                                if (displayName.isBlank()) {
                                    localError = "Please enter your display name"
                                    return@Button
                                }
                                if (secretCode.length < 4) {
                                    localError = "Secret code must be at least 4 digits"
                                    return@Button
                                }
                                isSubmitting = true
                                localError = null
                                onPairMember(inviteCodeInput, displayName, secretCode)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_member_pair"),
                            colors = ButtonDefaults.buttonColors(containerColor = IosGreen),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Authorize & Complete Pairing", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = onLogout) {
                            Text("Sign Out", color = IosTextSecondary)
                        }
                    }
                }
            }

            else -> {
                CircularProgressIndicator(color = IosBlue)
            }
        }
    }
}
