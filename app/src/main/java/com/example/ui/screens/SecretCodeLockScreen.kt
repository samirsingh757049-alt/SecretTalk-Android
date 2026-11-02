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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardBg
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurfaceVariant
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary

@Composable
fun SecretCodeLockScreen(
    enteredCode: String,
    errorMessage: String?,
    lockoutRemainingSeconds: Long,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(IosBlue.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = IosBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SecretTalk",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = IosTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (lockoutRemainingSeconds > 0) {
                        "Locked due to failed attempts"
                    } else {
                        "Enter Secret Access Code"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (lockoutRemainingSeconds > 0) IosRed else IosTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Passcode dots (4 to 6 digits)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < enteredCode.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) IosBlue else IosSurfaceVariant
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = IosRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (lockoutRemainingSeconds > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Try again in ${lockoutRemainingSeconds}s",
                        color = IosRed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                for (row in keypadRows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            when (key) {
                                "" -> {
                                    Spacer(modifier = Modifier.size(72.dp))
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = lockoutRemainingSeconds <= 0) {
                                                onDeleteClick()
                                            }
                                            .testTag("keypad_delete"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = IosTextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(IosCardBg)
                                            .clickable(enabled = lockoutRemainingSeconds <= 0) {
                                                onDigitClick(key)
                                            }
                                            .testTag("keypad_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = IosTextPrimary,
                                                fontSize = 28.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onLogoutClick) {
                    Text(
                        text = "Sign Out",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
