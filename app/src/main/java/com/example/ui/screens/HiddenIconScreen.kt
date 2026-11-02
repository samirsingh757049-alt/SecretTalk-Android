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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

    Scaffold(
        containerColor = IosBackground,
        topBar = {
            TopAppBar(
                title = { Text("Hidden Launcher Icon", color = IosTextPrimary, fontWeight = FontWeight.SemiBold) },
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
            // Main Toggle Card
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isIconHidden) IosOrange.copy(alpha = 0.2f) else IosBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isIconHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isIconHidden) IosOrange else IosBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Hide Launcher Icon",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = IosTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = if (isIconHidden) "Icon is currently HIDDEN" else "Icon is currently VISIBLE",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isIconHidden) IosOrange else IosGreen
                                )
                            )
                        }
                    }

                    Switch(
                        checked = isIconHidden,
                        onCheckedChange = { onToggleHideIcon(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IosBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = IosDivider
                        ),
                        modifier = Modifier.testTag("toggle_hide_icon_switch")
                    )
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

            // Recovery Instructions Card
            Text(
                text = "PERMANENT RECOVERY METHOD",
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
                            text = "Secret Deep Link Scheme",
                            color = IosTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "When the launcher icon is hidden, the app can always be opened by entering the custom URL in any browser or tapping a link:",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosSurface)
                            .padding(10.dp)
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
                text = "ANDROID LAUNCHER LIMITATIONS",
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
                        text = "Note: Android home screen launchers cache app drawer items. It may take 10-30 seconds or an app drawer refresh for the icon change to reflect. In some third-party launchers, a system restart flushes the icon cache.",
                        color = IosTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
