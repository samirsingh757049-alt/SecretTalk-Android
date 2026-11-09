package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.media.VoiceRecorderPlayer
import com.example.data.model.ChatMessage
import com.example.data.model.SystemPairing
import com.example.data.model.UserProfile
import com.example.data.repository.SecretTalkRepository
import com.example.data.security.AppPreferences
import com.example.data.security.CryptoManager
import com.example.data.webrtc.CallUiState
import com.example.data.webrtc.WebRtcCallManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PairingScreenState {
    data object Loading : PairingScreenState()
    data object UnregisteredOwner : PairingScreenState() // User A can register
    data object AwaitingUserB : PairingScreenState() // User A registered, User B can pair with code
    data object Paired : PairingScreenState() // Both users registered
    data class RegistrationClosed(val message: String) : PairingScreenState() // Extra 3rd user rejected
}

data class DecryptedMessage(
    val message: ChatMessage,
    val plainText: String,
    val isFromMe: Boolean
)

class SecretTalkViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SecretTalkRepository(application)
    val preferences = AppPreferences(application)
    val voicePlayer = VoiceRecorderPlayer(application)
    val callManager = WebRtcCallManager(application, repository)

    private val auth = Firebase.auth

    // Auth State
    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Passcode Lock State
    val isLocked: StateFlow<Boolean> = preferences.isLockedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining: StateFlow<Long> = _lockoutTimeRemaining.asStateFlow()

    private val _enteredPasscode = MutableStateFlow("")
    val enteredPasscode: StateFlow<String> = _enteredPasscode.asStateFlow()

    private val _passcodeError = MutableStateFlow<String?>(null)
    val passcodeError: StateFlow<String?> = _passcodeError.asStateFlow()

    // Pairing State
    private val _pairingState = MutableStateFlow<PairingScreenState>(PairingScreenState.Loading)
    val pairingState: StateFlow<PairingScreenState> = _pairingState.asStateFlow()

    private val _currentPairing = MutableStateFlow<SystemPairing?>(null)
    val currentPairing: StateFlow<SystemPairing?> = _currentPairing.asStateFlow()

    // Profiles
    private val _myProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile.asStateFlow()

    private val _partnerProfile = MutableStateFlow<UserProfile?>(null)
    val partnerProfile: StateFlow<UserProfile?> = _partnerProfile.asStateFlow()

    // Chat
    private val _rawMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _activeSecretCode = MutableStateFlow("")

    val decryptedMessages: StateFlow<List<DecryptedMessage>> = combine(
        _rawMessages,
        _activeSecretCode,
        _currentUserId
    ) { messages, secretKey, uid ->
        messages.map { msg ->
            val plain = if (secretKey.isNotBlank()) {
                CryptoManager.decryptMessage(msg.encryptedText, secretKey)
            } else {
                "[Encrypted Message]"
            }
            DecryptedMessage(
                message = msg,
                plainText = plain,
                isFromMe = msg.senderId == uid
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Hidden Launcher Icon
    val isIconHidden: StateFlow<Boolean> = preferences.isIconHiddenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    // UI Status / Feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        checkSession()
        listenForAuthState()
    }

    private fun listenForAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            _currentUserId.value = uid
            if (uid != null) {
                initializeUserSession(uid)
            } else {
                _pairingState.value = PairingScreenState.Loading
                _myProfile.value = null
                _partnerProfile.value = null
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val lockoutUntil = preferences.getLockoutUntil()
            val now = System.currentTimeMillis()
            if (lockoutUntil > now) {
                _lockoutTimeRemaining.value = (lockoutUntil - now) / 1000L
            }
            val savedCode = preferences.getActiveSecretCode()
            _activeSecretCode.value = savedCode
        }
    }

    fun initializeUserSession(uid: String) {
        viewModelScope.launch {
            // Observe pairing configuration
            repository.observePairing().collect { pairing ->
                _currentPairing.value = pairing
                if (pairing == null) {
                    // No pairing doc exists yet -> First user is Owner (User A)
                    _pairingState.value = PairingScreenState.UnregisteredOwner
                } else {
                    val isUserA = pairing.userAId == uid
                    val isUserB = pairing.userBId == uid

                    if (isUserA || isUserB) {
                        _pairingState.value = PairingScreenState.Paired
                        // Load profiles
                        val partnerUid = if (isUserA) pairing.userBId else pairing.userAId
                        observeProfiles(uid, partnerUid)
                        observeMessages()
                        observeCalls()
                    } else if (pairing.registeredCount == 1L && !pairing.isLocked) {
                        // User A is registered, User B can pair
                        _pairingState.value = PairingScreenState.AwaitingUserB
                    } else {
                        // Exactly two accounts already registered -> Reject 3rd user!
                        _pairingState.value = PairingScreenState.RegistrationClosed(
                            "Access Denied: Exactly two registered accounts are permitted on this private messenger."
                        )
                    }
                }
            }
        }
    }

    private fun observeProfiles(myUid: String, partnerUid: String?) {
        viewModelScope.launch {
            repository.observeUserProfile(myUid).collect { profile ->
                _myProfile.value = profile
            }
        }
        if (partnerUid != null && partnerUid.isNotBlank()) {
            viewModelScope.launch {
                repository.observeUserProfile(partnerUid).collect { profile ->
                    _partnerProfile.value = profile
                }
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.observeMessages().collect { msgs ->
                _rawMessages.value = msgs
                // Auto mark incoming unread messages as read
                val myUid = _currentUserId.value
                msgs.filter { it.receiverId == myUid && it.status != "read" }.forEach {
                    repository.markMessageRead(it.id)
                }
            }
        }
    }

    private fun observeCalls() {
        viewModelScope.launch {
            repository.observeActiveCalls().collect { activeCalls ->
                val myUid = _currentUserId.value
                val incoming = activeCalls.firstOrNull { it.receiverId == myUid && it.status == "ringing" }
                if (incoming != null && callManager.callState.value == CallUiState.IDLE) {
                    val partnerName = _partnerProfile.value?.displayName ?: "Partner"
                    callManager.handleIncomingCall(incoming.id, partnerName, incoming.callType)
                }
            }
        }
    }

    // --- Passcode / Lock Screen Actions ---

    fun onPasscodeDigit(digit: String) {
        if (_lockoutTimeRemaining.value > 0) return
        if (_enteredPasscode.value.length < 6) {
            val updated = _enteredPasscode.value + digit
            _enteredPasscode.value = updated
            _passcodeError.value = null
            if (updated.length >= 4) {
                // Check code
                verifyEnteredPasscode(updated)
            }
        }
    }

    fun onPasscodeDelete() {
        if (_enteredPasscode.value.isNotEmpty()) {
            _enteredPasscode.value = _enteredPasscode.value.dropLast(1)
            _passcodeError.value = null
        }
    }

    private fun verifyEnteredPasscode(code: String) {
        viewModelScope.launch {
            val isValid = repository.verifySecretCode(code)
            if (isValid) {
                preferences.resetFailedAttempts()
                preferences.setActiveSecretCode(code)
                _activeSecretCode.value = code
                preferences.setLocked(false)
                _enteredPasscode.value = ""
                _passcodeError.value = null
            } else {
                val (attempts, lockoutUntil) = preferences.recordFailedAttempt()
                _enteredPasscode.value = ""
                if (attempts >= 5) {
                    val remainingSec = (lockoutUntil - System.currentTimeMillis()) / 1000L
                    _lockoutTimeRemaining.value = remainingSec.coerceAtLeast(1L)
                    _passcodeError.value = "Too many failed attempts. Locked for 30s."
                } else {
                    _passcodeError.value = "Incorrect Secret Code (${5 - attempts} attempts left)"
                }
            }
        }
    }

    fun lockNow() {
        viewModelScope.launch {
            preferences.setLocked(true)
            preferences.setActiveSecretCode("")
            _activeSecretCode.value = ""
            _enteredPasscode.value = ""
        }
    }

    // --- Owner & Member Registration ---

    fun registerAsOwner(displayName: String, secretCode: String, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerOwner(displayName.trim(), secretCode.trim())
            if (result.isSuccess) {
                preferences.setActiveSecretCode(secretCode.trim())
                _activeSecretCode.value = secretCode.trim()
                preferences.setLocked(false)
            }
            onComplete(result)
        }
    }

    fun pairWithInviteCode(inviteCode: String, displayName: String, secretCode: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.pairWithInviteCode(inviteCode.trim().uppercase(), displayName.trim(), secretCode.trim())
            if (result.isSuccess) {
                preferences.setActiveSecretCode(secretCode.trim())
                _activeSecretCode.value = secretCode.trim()
                preferences.setLocked(false)
            }
            onComplete(result)
        }
    }

    // --- Messaging ---

    fun sendMessage(text: String) {
        val partnerUid = getPartnerUid() ?: return
        val secretKey = _activeSecretCode.value
        if (text.isBlank() || secretKey.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                receiverId = partnerUid,
                plainText = text.trim(),
                secretKey = secretKey,
                mediaType = "text"
            )
        }
    }

    fun sendVoiceNote(base64Audio: String, duration: Long) {
        val partnerUid = getPartnerUid() ?: return
        val secretKey = _activeSecretCode.value
        viewModelScope.launch {
            repository.sendMessage(
                receiverId = partnerUid,
                plainText = "[Voice Note - ${duration}s]",
                secretKey = secretKey,
                mediaType = "audio",
                mediaUrl = base64Audio,
                audioDuration = duration
            )
        }
    }

    fun sendMediaAttachment(base64Data: String, isVideo: Boolean) {
        val partnerUid = getPartnerUid() ?: return
        val secretKey = _activeSecretCode.value
        viewModelScope.launch {
            repository.sendMessage(
                receiverId = partnerUid,
                plainText = if (isVideo) "[Video Attachment]" else "[Photo Attachment]",
                secretKey = secretKey,
                mediaType = if (isVideo) "video" else "image",
                mediaUrl = base64Data
            )
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            repository.deleteMessage(messageId, forEveryone)
        }
    }

    fun setTyping(isTyping: Boolean) {
        viewModelScope.launch {
            repository.updatePresence(isOnline = true, isTyping = isTyping)
        }
    }

    // --- Audio & Video Calling ---

    fun startCall(isVideo: Boolean) {
        val partnerUid = getPartnerUid() ?: return
        val partnerName = _partnerProfile.value?.displayName ?: "Partner"
        callManager.startOutgoingCall(partnerUid, partnerName, if (isVideo) "video" else "audio")
    }

    private fun getPartnerUid(): String? {
        val pairing = _currentPairing.value ?: return null
        val myUid = _currentUserId.value ?: return null
        return if (pairing.userAId == myUid) pairing.userBId else pairing.userAId
    }

    // --- Hidden Launcher Icon ---

    fun toggleLauncherIcon(hide: Boolean) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val pm = app.packageManager
            val alias1 = ComponentName(app, "com.example.MainActivityAlias")
            val alias2 = ComponentName(app.packageName, "com.example.MainActivityAlias")
            val alias3 = ComponentName(app.packageName, "${app.packageName}.MainActivityAlias")

            val newState = if (hide) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            var updated = false
            for (alias in listOf(alias1, alias2, alias3)) {
                try {
                    pm.setComponentEnabledSetting(alias, newState, PackageManager.DONT_KILL_APP)
                    updated = true
                } catch (ignored: Exception) {}
            }

            if (updated) {
                preferences.setIconHidden(hide)
                _statusMessage.value = if (hide) {
                    "Launcher icon hidden! Dial *#*#7327#*#* or open secrettalk://open to reopen."
                } else {
                    "Launcher icon restored to home screen and app drawer."
                }
            } else {
                _statusMessage.value = "Failed to update icon component setting."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateSecretAccessCode(newCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateSecretCode(newCode.trim())
            if (res.isSuccess) {
                preferences.setActiveSecretCode(newCode.trim())
                _activeSecretCode.value = newCode.trim()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updatePresence(isOnline = false, isTyping = false)
            preferences.clearSession()
            auth.signOut()
            onComplete()
        }
    }
}
