package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun HiddenIconScreen(
    isIconHidden: Boolean,
    statusFeedback: String?,
    onToggleHideIcon: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showConfirmHideDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = IosBackground,
        topBar = {
            TopAppBar(
                title = { Text("APK Hide Feature", color = IosTextPrimary, fontWeight = FontWeight.SemiBold) },
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
            // Hero Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isIconHidden) IosOrange.copy(alpha = 0.15f) else IosGreen.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isIconHidden) IosOrange.copy(alpha = 0.4f) else IosGreen.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isIconHidden) IosOrange.copy(alpha = 0.25f) else IosGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIconHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isIconHidden) IosOrange else IosGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isIconHidden) "STATUS: APP HIDDEN" else "STATUS: APP VISIBLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isIconHidden) IosOrange else IosGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = if (isIconHidden)
                                "The app icon is removed from the launcher & app drawer."
                            else
                                "The app icon is currently visible on your home screen.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IosTextSecondary
                            )
                        )
                    }
                }
            }

            // Main Toggle Card
            Text(
                text = "STEALTH CONTROL",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide App from Launcher",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = IosTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Disables launcher activity alias on device",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = IosTextSecondary
                                )
                            )
                        }

                        Switch(
                            checked = isIconHidden,
                            onCheckedChange = { willHide ->
                                if (willHide) {
                                    showConfirmHideDialog = true
                                } else {
                                    onToggleHideIcon(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IosOrange,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = IosDivider
                            ),
                            modifier = Modifier.testTag("toggle_hide_icon_switch")
                        )
                    }

                    if (isIconHidden) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onToggleHideIcon(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = IosGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore App Icon to Home Screen", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (statusFeedback != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IosSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusFeedback,
                        color = IosBlue,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Method 1: Secret Dialer Code
            Text(
                text = "RECOVERY METHOD 1: PHONE DIALER CODE",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = IosGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secret Dialer Code (No Icon Needed)",
                            color = IosTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Open your Android Phone / Dialer app and dial either code to immediately launch SecretTalk:",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosSurface)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "*#*#7327#*#*",
                                color = IosGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosSurface)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "*#*#1234#*#*",
                                color = IosGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*%23*%237327%23*%23*"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = IosGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Phone Dialer with Code", color = IosTextPrimary)
                    }
                }
            }

            // Method 2: Browser Deep Link
            Text(
                text = "RECOVERY METHOD 2: BROWSER DEEP LINK",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browser Address Bar Link",
                            color = IosTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Type this URI into Chrome or tap any link formatted with this address:",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosSurface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "secrettalk://open",
                            color = IosBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("secrettalk://open"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Opening via Deep Link", color = IosTextPrimary)
                    }
                }
            }

            // Android Launcher Limitations Card
            Text(
                text = "IMPORTANT LAUNCHER NOTES",
                style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = IosOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Android home launchers cache app icons. When hiding or restoring the icon, it may take 10-30 seconds for the home screen to refresh, or you can swipe down to refresh the app drawer.",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (showConfirmHideDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmHideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = IosOrange,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Hide App Icon from Home Screen?",
                    color = IosTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SecretTalk will disappear from your phone's launcher and app drawer.",
                        color = IosTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "You can reopen SecretTalk anytime by:",
                        color = IosTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Dialing *#*#7327#*#* in Phone app\n• Opening secrettalk://open in Chrome",
                        color = IosBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmHideDialog = false
                        onToggleHideIcon(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosOrange)
                ) {
                    Text("Confirm & Hide", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmHideDialog = false }) {
                    Text("Cancel", color = IosTextSecondary)
                }
            },
            containerColor = IosCardBg
        )
    }
}
