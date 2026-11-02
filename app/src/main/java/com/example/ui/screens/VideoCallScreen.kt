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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.webrtc.CallUiState
import com.example.data.webrtc.WebRtcCallManager
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurfaceVariant
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    callManager: WebRtcCallManager,
    callState: CallUiState,
    partnerName: String,
    durationSeconds: Long,
    isMicMuted: Boolean,
    isCameraEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationFormatted = "%02d:%02d".format(minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
    ) {
        // Remote Video (Full Screen)
        if (callState == CallUiState.CONNECTED && callManager.remoteVideoTrack != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(callManager.eglBase.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                        callManager.remoteVideoTrack?.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder while ringing or connecting
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(IosSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partnerName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = partnerName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = IosTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusText = when (callState) {
                        CallUiState.OUTGOING_RINGING -> "Calling (Video)..."
                        CallUiState.INCOMING_RINGING -> "Incoming Encrypted Video Call"
                        CallUiState.CONNECTING -> "Connecting Video..."
                        CallUiState.CONNECTED -> durationFormatted
                        CallUiState.ENDED -> "Call Ended"
                        CallUiState.IDLE -> ""
                    }

                    Text(
                        text = statusText,
                        color = if (callState == CallUiState.CONNECTED) IosGreen else IosTextSecondary
                    )
                }
            }
        }

        // Local Camera PiP (Top-Right)
        if (isCameraEnabled && callManager.localVideoTrack != null && callState != CallUiState.INCOMING_RINGING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 20.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(callManager.eglBase.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setMirror(true)
                            callManager.localVideoTrack?.addSink(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Call Duration Pill at top
        if (callState == CallUiState.CONNECTED) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = durationFormatted,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Controls at Bottom
        if (callState == CallUiState.INCOMING_RINGING) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
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
                            .testTag("reject_video_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Video Call",
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
                            .testTag("accept_video_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Accept Video Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accept", color = IosGreen, fontSize = 13.sp)
                }
            }
        } else {
            // Floating Glassmorphic Control Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch Camera
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(IosSurfaceVariant)
                            .clickable { onSwitchCamera() }
                            .testTag("switch_camera_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Camera On/Off
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isCameraEnabled) IosSurfaceVariant else Color.White)
                            .clickable { onToggleCamera() }
                            .testTag("toggle_camera_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Video",
                            tint = if (isCameraEnabled) Color.White else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Mic Mute
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) Color.White else IosSurfaceVariant)
                            .clickable { onToggleMic() }
                            .testTag("toggle_mic_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic",
                            tint = if (isMicMuted) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // End Call
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(IosRed)
                            .clickable { onEndCall() }
                            .testTag("end_video_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
