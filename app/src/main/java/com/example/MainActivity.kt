package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.webrtc.CallUiState
import com.example.ui.PairingScreenState
import com.example.ui.SecretTalkViewModel
import com.example.ui.screens.AccountSetupScreen
import com.example.ui.screens.AudioCallScreen
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.screens.HiddenIconScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.PrivateChatScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SecretCodeLockScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.IosBackground
import com.example.ui.theme.SecretTalkTheme

enum class AppSubScreen {
    CHAT,
    PRIVACY_SETTINGS,
    PROFILE,
    HIDDEN_ICON
}

class MainActivity : ComponentActivity() {

    private val viewModel: SecretTalkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecretTalkTheme {
                MainAppNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setTyping(false)
    }

    override fun onPause() {
        super.onPause()
        viewModel.setTyping(false)
    }
}

@Composable
fun MainAppNavigation(viewModel: SecretTalkViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var currentSubScreen by remember { mutableStateOf(AppSubScreen.CHAT) }
    var previousSubScreen by remember { mutableStateOf(AppSubScreen.CHAT) }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val lockoutRemaining by viewModel.lockoutTimeRemaining.collectAsState()
    val enteredPasscode by viewModel.enteredPasscode.collectAsState()
    val passcodeError by viewModel.passcodeError.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val partnerProfile by viewModel.partnerProfile.collectAsState()
    val currentPairing by viewModel.currentPairing.collectAsState()
    val messages by viewModel.decryptedMessages.collectAsState()
    val isIconHidden by viewModel.isIconHidden.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val callState by viewModel.callManager.callState.collectAsState()
    val callType by viewModel.callManager.callType.collectAsState()
    val partnerCallName by viewModel.callManager.partnerName.collectAsState()
    val callDuration by viewModel.callManager.callDurationSeconds.collectAsState()
    val isMicMuted by viewModel.callManager.isMicMuted.collectAsState()
    val isCameraEnabled by viewModel.callManager.isCameraEnabled.collectAsState()
    val isSpeakerOn by viewModel.callManager.isSpeakerOn.collectAsState()

    // Runtime permission launcher for calls and voice recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    // 1. Google Authentication Gate
    if (currentUserId == null) {
        GoogleSignInScreen(
            onAuthSuccess = {
                // Auth listener updates currentUserId
            }
        )
        return
    }

    // 2. Secret Code Lock Gate
    if (isLocked) {
        SecretCodeLockScreen(
            enteredCode = enteredPasscode,
            errorMessage = passcodeError,
            lockoutRemainingSeconds = lockoutRemaining,
            onDigitClick = { viewModel.onPasscodeDigit(it) },
            onDeleteClick = { viewModel.onPasscodeDelete() },
            onLogoutClick = {
                viewModel.logout {}
            }
        )
        return
    }

    // 3. Pairing / Provisioning Gate
    if (pairingState !is PairingScreenState.Paired) {
        AccountSetupScreen(
            pairingState = pairingState,
            onRegisterOwner = { name, code ->
                viewModel.registerAsOwner(name, code) {}
            },
            onPairMember = { invite, name, code ->
                viewModel.pairWithInviteCode(invite, name, code) {}
            },
            onLogout = {
                viewModel.logout {}
            }
        )
        return
    }

    // 4. Active Call Screen Gate (Audio or Video)
    if (callState != CallUiState.IDLE) {
        if (callType == "video") {
            VideoCallScreen(
                callManager = viewModel.callManager,
                callState = callState,
                partnerName = partnerCallName,
                durationSeconds = callDuration,
                isMicMuted = isMicMuted,
                isCameraEnabled = isCameraEnabled,
                onToggleMic = { viewModel.callManager.toggleMicMute() },
                onToggleCamera = { viewModel.callManager.toggleCamera() },
                onSwitchCamera = { viewModel.callManager.switchCamera() },
                onAcceptCall = {
                    viewModel.callManager.acceptCall("")
                },
                onRejectCall = { viewModel.callManager.rejectCall() },
                onEndCall = { viewModel.callManager.endCall() }
            )
        } else {
            AudioCallScreen(
                partnerName = partnerCallName,
                callState = callState,
                durationSeconds = callDuration,
                isMicMuted = isMicMuted,
                isSpeakerOn = isSpeakerOn,
                onToggleMic = { viewModel.callManager.toggleMicMute() },
                onToggleSpeaker = { viewModel.callManager.toggleSpeakerphone() },
                onAcceptCall = {
                    viewModel.callManager.acceptCall("")
                },
                onRejectCall = { viewModel.callManager.rejectCall() },
                onEndCall = { viewModel.callManager.endCall() }
            )
        }
        return
    }

    // 5. Main Chat and Sub-Screens
    when (currentSubScreen) {
        AppSubScreen.CHAT -> {
            PrivateChatScreen(
                partnerProfile = partnerProfile,
                messages = messages,
                voicePlayer = viewModel.voicePlayer,
                onSendMessage = { viewModel.sendMessage(it) },
                onSendVoiceNote = { audio, duration ->
                    viewModel.sendVoiceNote(audio, duration)
                },
                onSendMedia = { data, isVideo ->
                    viewModel.sendMediaAttachment(data, isVideo)
                },
                onDeleteMessage = { id, forEveryone ->
                    viewModel.deleteMessage(id, forEveryone)
                },
                onStartCall = { isVideo ->
                    viewModel.startCall(isVideo)
                },
                onOpenSettings = {
                    previousSubScreen = AppSubScreen.CHAT
                    currentSubScreen = AppSubScreen.PRIVACY_SETTINGS
                },
                onOpenProfile = {
                    previousSubScreen = AppSubScreen.CHAT
                    currentSubScreen = AppSubScreen.PROFILE
                },
                onOpenHiddenIcon = {
                    previousSubScreen = AppSubScreen.CHAT
                    currentSubScreen = AppSubScreen.HIDDEN_ICON
                },
                isIconHidden = isIconHidden,
                onTypingChange = { viewModel.setTyping(it) }
            )
        }

        AppSubScreen.PRIVACY_SETTINGS -> {
            BackHandler {
                currentSubScreen = AppSubScreen.CHAT
            }
            PrivacySettingsScreen(
                onNavigateBack = { currentSubScreen = AppSubScreen.CHAT },
                onNavigateToHiddenIcon = {
                    previousSubScreen = AppSubScreen.PRIVACY_SETTINGS
                    currentSubScreen = AppSubScreen.HIDDEN_ICON
                },
                onLockNow = { viewModel.lockNow() },
                onLogout = {
                    viewModel.logout {}
                },
                onChangeSecretCode = { newCode, onDone ->
                    viewModel.updateSecretAccessCode(newCode, onDone)
                }
            )
        }

        AppSubScreen.PROFILE -> {
            BackHandler {
                currentSubScreen = AppSubScreen.CHAT
            }
            ProfileScreen(
                myProfile = myProfile,
                partnerProfile = partnerProfile,
                currentPairing = currentPairing,
                onNavigateBack = { currentSubScreen = AppSubScreen.CHAT }
            )
        }

        AppSubScreen.HIDDEN_ICON -> {
            BackHandler {
                currentSubScreen = previousSubScreen
            }
            HiddenIconScreen(
                isIconHidden = isIconHidden,
                statusFeedback = statusMessage,
                onToggleHideIcon = { viewModel.toggleLauncherIcon(it) },
                onNavigateBack = { currentSubScreen = previousSubScreen }
            )
        }
    }
}
