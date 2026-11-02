package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.webrtc.CallUiState
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurfaceVariant
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary

@Composable
fun AudioCallScreen(
    partnerName: String,
    callState: CallUiState,
    durationSeconds: Long,
    isMicMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(callState) {
        if (callState == CallUiState.OUTGOING_RINGING || callState == CallUiState.INCOMING_RINGING) {
            pulseScale.animateTo(
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationFormatted = "%02d:%02d".format(minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = IosTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (callState) {
                    CallUiState.OUTGOING_RINGING -> "Calling..."
                    CallUiState.INCOMING_RINGING -> "Incoming Encrypted Audio Call"
                    CallUiState.CONNECTING -> "Connecting..."
                    CallUiState.CONNECTED -> durationFormatted
                    CallUiState.ENDED -> "Call Ended"
                    CallUiState.IDLE -> ""
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (callState == CallUiState.CONNECTED) IosGreen else IosTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Pulsing Avatar
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale.value)
                        .clip(CircleShape)
                        .background(IosSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partnerName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Controls
            if (callState == CallUiState.INCOMING_RINGING) {
                // Incoming Call: Accept / Decline
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(IosRed)
                                .clickable { onRejectCall() }
                                .testTag("reject_audio_call_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = IosTextSecondary, fontSize = 13.sp)
                    }

                    // Accept
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(IosGreen)
                                .clickable { onAcceptCall() }
                                .testTag("accept_audio_call_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", color = IosGreen, fontSize = 13.sp)
                    }
                }
            } else {
                // In Call / Outgoing Call Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mic Mute
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isMicMuted) Color.White else IosSurfaceVariant)
                                    .clickable { onToggleMic() }
                                    .testTag("mute_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMicMuted) Color.Black else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (isMicMuted) "Unmute" else "Mute", color = IosTextSecondary, fontSize = 12.sp)
                        }

                        // Speakerphone
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerOn) Color.White else IosSurfaceVariant)
                                    .clickable { onToggleSpeaker() }
                                    .testTag("speaker_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = if (isSpeakerOn) Color.Black else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Speaker", color = IosTextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Red End Call Pill
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(IosRed)
                            .clickable { onEndCall() }
                            .testTag("end_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}
