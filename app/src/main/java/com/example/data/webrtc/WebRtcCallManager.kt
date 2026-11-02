package com.example.data.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.example.data.repository.SecretTalkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class CallUiState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED,
    ENDED
}

class WebRtcCallManager(
    private val context: Context,
    private val repository: SecretTalkRepository
) {
    val eglBase: EglBase = EglBase.create()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main)

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    var localVideoTrack: VideoTrack? = null
        private set
    var remoteVideoTrack: VideoTrack? = null
        private set
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var currentCallId: String? = null
    private var isCaller: Boolean = false
    private var callTimerJob: Job? = null

    private val _callState = MutableStateFlow(CallUiState.IDLE)
    val callState: StateFlow<CallUiState> = _callState.asStateFlow()

    private val _callType = MutableStateFlow("audio") // "audio" or "video"
    val callType: StateFlow<String> = _callType.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val _partnerName = MutableStateFlow("Partner")
    val partnerName: StateFlow<String> = _partnerName.asStateFlow()

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection(): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        return peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                currentCallId?.let { cid ->
                    scope.launch {
                        val candidateJson = "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}"
                        repository.appendIceCandidate(cid, isCaller, candidateJson)
                    }
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        _callState.value = CallUiState.CONNECTED
                        startCallTimer()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        // Reconnection attempt handled by WebRTC, or end call if disconnected
                    }
                    PeerConnection.IceConnectionState.CLOSED -> {
                        _callState.value = CallUiState.ENDED
                    }
                    else -> {}
                }
            }

            override fun onAddStream(stream: MediaStream?) {
                if (stream != null && stream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = stream.videoTracks[0]
                }
            }

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    fun startOutgoingCall(receiverId: String, partnerDisplayName: String, type: String) {
        _callType.value = type
        _partnerName.value = partnerDisplayName
        _callState.value = CallUiState.OUTGOING_RINGING
        _callDurationSeconds.value = 0L
        isCaller = true

        setupAudio(type == "video")
        setupLocalMedia(type == "video")

        peerConnection = createPeerConnection()
        localAudioTrack?.let { peerConnection?.addTrack(it) }
        if (type == "video") {
            localVideoTrack?.let { peerConnection?.addTrack(it) }
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            if (type == "video") {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { offer ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            scope.launch {
                                val res = repository.initiateCall(receiverId, type, offer.description)
                                res.onSuccess { callId ->
                                    currentCallId = callId
                                    observeCallUpdates(callId)
                                }.onFailure {
                                    endCall()
                                }
                            }
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, offer)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { endCall() }
            override fun onSetFailure(p0: String?) { endCall() }
        }, constraints)
    }

    fun handleIncomingCall(callId: String, callerDisplayName: String, type: String) {
        currentCallId = callId
        isCaller = false
        _callType.value = type
        _partnerName.value = callerDisplayName
        _callState.value = CallUiState.INCOMING_RINGING
        _callDurationSeconds.value = 0L
    }

    fun acceptCall(offerSdp: String) {
        val cid = currentCallId ?: return
        _callState.value = CallUiState.CONNECTING
        val isVideo = _callType.value == "video"

        setupAudio(isVideo)
        setupLocalMedia(isVideo)

        peerConnection = createPeerConnection()
        localAudioTrack?.let { peerConnection?.addTrack(it) }
        if (isVideo) {
            localVideoTrack?.let { peerConnection?.addTrack(it) }
        }

        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    if (isVideo) {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                    }
                }
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let { answer ->
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    scope.launch {
                                        repository.answerCall(cid, answer.description)
                                        observeCallUpdates(cid)
                                    }
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            }, answer)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) { endCall() }
                    override fun onSetFailure(p0: String?) { endCall() }
                }, constraints)
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) { endCall() }
            override fun onSetFailure(p0: String?) { endCall() }
        }, remoteDesc)
    }

    fun rejectCall() {
        val cid = currentCallId
        if (cid != null) {
            scope.launch {
                repository.endCall(cid, 0L)
            }
        }
        endCall()
    }

    private fun observeCallUpdates(callId: String) {
        scope.launch {
            repository.observeCall(callId).collect { session ->
                if (session == null || session.status == "ended" || session.status == "rejected") {
                    endCall()
                    return@collect
                }

                if (isCaller && session.status == "accepted" && session.answerSdp.isNotEmpty() &&
                    peerConnection?.remoteDescription == null) {
                    val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, session.answerSdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            _callState.value = CallUiState.CONNECTED
                            startCallTimer()
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, remoteDesc)
                }

                // Parse candidate exchange
                val candidatesString = if (isCaller) session.receiverCandidates else session.callerCandidates
                if (candidatesString.isNotEmpty()) {
                    val parts = candidatesString.split("|")
                    if (parts.size >= 3) {
                        try {
                            val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
                            peerConnection?.addIceCandidate(candidate)
                        } catch (ignored: Exception) {}
                    }
                }
            }
        }
    }

    private fun setupAudio(isSpeakerDefault: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphone(isSpeakerDefault)

        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)
    }

    private fun setupLocalMedia(enableVideo: Boolean) {
        if (!enableVideo) return

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Prefer front camera for video calls
        val frontCamera = deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: deviceNames.firstOrNull()
        if (frontCamera != null) {
            videoCapturer = enumerator.createCapturer(frontCamera, null)
            videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)

            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
        }
    }

    fun toggleMicMute() {
        val newMute = !_isMicMuted.value
        _isMicMuted.value = newMute
        localAudioTrack?.setEnabled(!newMute)
    }

    fun toggleCamera() {
        val newCameraState = !_isCameraEnabled.value
        _isCameraEnabled.value = newCameraState
        localVideoTrack?.setEnabled(newCameraState)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun setSpeakerphone(on: Boolean) {
        _isSpeakerOn.value = on
        audioManager.isSpeakerphoneOn = on
    }

    fun toggleSpeakerphone() {
        setSpeakerphone(!_isSpeakerOn.value)
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        _callDurationSeconds.value = 0L
        callTimerJob = scope.launch {
            while (isActive && _callState.value == CallUiState.CONNECTED) {
                delay(1000L)
                _callDurationSeconds.value += 1L
            }
        }
    }

    fun endCall() {
        callTimerJob?.cancel()
        val duration = _callDurationSeconds.value
        currentCallId?.let { cid ->
            scope.launch {
                repository.endCall(cid, duration)
            }
        }

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            localVideoTrack?.dispose()
            localVideoTrack = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            peerConnection?.close()
            peerConnection = null

            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (ignored: Exception) {}

        _callState.value = CallUiState.IDLE
        currentCallId = null
    }
}
