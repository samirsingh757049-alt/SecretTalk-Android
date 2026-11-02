package com.example.data.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VoiceRecorderPlayer(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordDurationSeconds = MutableStateFlow(0)
    val recordDurationSeconds: StateFlow<Int> = _recordDurationSeconds.asStateFlow()

    private val _activePlayingMessageId = MutableStateFlow<String?>(null)
    val activePlayingMessageId: StateFlow<String?> = _activePlayingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var recordTimerJob: Job? = null
    private var playProgressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(): Boolean {
        return try {
            val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            currentRecordingFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(currentRecordingFile!!.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _recordDurationSeconds.value = 0

            recordTimerJob = scope.launch {
                while (isActive && _isRecording.value) {
                    delay(1000L)
                    _recordDurationSeconds.value += 1
                }
            }
            true
        } catch (e: Exception) {
            _isRecording.value = false
            currentRecordingFile = null
            false
        }
    }

    fun stopRecording(): Pair<String, Long>? {
        recordTimerJob?.cancel()
        val duration = _recordDurationSeconds.value.toLong().coerceAtLeast(1L)
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false

            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                val bytes = ByteArray(file.length().toInt())
                FileInputStream(file).use { it.read(bytes) }
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Pair(base64, duration)
            } else {
                null
            }
        } catch (e: Exception) {
            mediaRecorder = null
            _isRecording.value = false
            null
        }
    }

    fun cancelRecording() {
        recordTimerJob?.cancel()
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (ignored: Exception) {}
        mediaRecorder = null
        _isRecording.value = false
        currentRecordingFile?.delete()
        currentRecordingFile = null
    }

    fun playAudio(messageId: String, base64Audio: String) {
        if (_activePlayingMessageId.value == messageId) {
            stopPlayback()
            return
        }
        stopPlayback()

        try {
            val audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            val tempFile = File(context.cacheDir, "play_$messageId.m4a")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }
            mediaPlayer = player
            _activePlayingMessageId.value = messageId

            player.setOnCompletionListener {
                stopPlayback()
                tempFile.delete()
            }

            playProgressJob = scope.launch {
                val duration = player.duration.toFloat()
                while (isActive && player.isPlaying) {
                    if (duration > 0) {
                        _playbackProgress.value = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                    }
                    delay(100L)
                }
            }
        } catch (e: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playProgressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null
        _activePlayingMessageId.value = null
        _playbackProgress.value = 0f
    }
}
