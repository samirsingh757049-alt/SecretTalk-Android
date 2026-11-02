package com.example.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.media.VoiceRecorderPlayer
import com.example.data.model.UserProfile
import com.example.ui.DecryptedMessage
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosBubbleReceived
import com.example.ui.theme.IosBubbleSent
import com.example.ui.theme.IosCardBg
import com.example.ui.theme.IosDivider
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSurface
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    partnerProfile: UserProfile?,
    messages: List<DecryptedMessage>,
    voicePlayer: VoiceRecorderPlayer,
    onSendMessage: (String) -> Unit,
    onSendVoiceNote: (base64Audio: String, duration: Long) -> Unit,
    onSendMedia: (base64Data: String, isVideo: Boolean) -> Unit,
    onDeleteMessage: (messageId: String, forEveryone: Boolean) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onTypingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messageInput by remember { mutableStateOf("") }
    var selectedMessageForOptions by remember { mutableStateOf<DecryptedMessage?>(null) }

    val isRecording by voicePlayer.isRecording.collectAsState()
    val recordDuration by voicePlayer.recordDurationSeconds.collectAsState()
    val activePlayingId by voicePlayer.activePlayingMessageId.collectAsState()
    val playbackProgress by voicePlayer.playbackProgress.collectAsState()

    // Photo picker launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onSendMedia(base64, false)
                }
            } catch (ignored: Exception) {}
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = IosBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onOpenProfile() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(IosBlue.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (partnerProfile?.displayName?.take(1) ?: "P").uppercase(),
                                color = IosBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = partnerProfile?.displayName ?: "Private Partner",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = IosTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (partnerProfile?.isOnline == true) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(IosGreen)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (partnerProfile.isTyping) "typing..." else "Online",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (partnerProfile.isTyping) IosBlue else IosGreen
                                        )
                                    )
                                } else {
                                    val lastSeenText = partnerProfile?.lastSeen?.toDate()?.let {
                                        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        "Last seen ${fmt.format(it)}"
                                    } ?: "Offline"
                                    Text(
                                        text = lastSeenText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = IosTextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onStartCall(false) },
                        modifier = Modifier.testTag("audio_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Audio Call",
                            tint = IosBlue
                        )
                    }

                    IconButton(
                        onClick = { onStartCall(true) },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = IosBlue
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("chat_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Privacy Settings",
                            tint = IosTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IosSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // E2EE Notice Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IosCardBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = IosTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Messages are end-to-end encrypted with AES-256",
                            style = MaterialTheme.typography.labelSmall.copy(color = IosTextSecondary)
                        )
                    }
                }
            }

            // Message Bubble List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.message.id }) { item ->
                    MessageBubble(
                        item = item,
                        isPlaying = activePlayingId == item.message.id,
                        playProgress = if (activePlayingId == item.message.id) playbackProgress else 0f,
                        onPlayAudio = {
                            voicePlayer.playAudio(item.message.id, item.message.mediaUrl)
                        },
                        onLongClick = {
                            selectedMessageForOptions = item
                        }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = IosSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Recording Indicator
                    AnimatedVisibility(visible = isRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IosRed.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(IosRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recording voice note: ${recordDuration}s",
                                    color = IosRed,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            TextButton(onClick = { voicePlayer.cancelRecording() }) {
                                Text("Cancel", color = IosTextSecondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media picker (+)
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.testTag("attach_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach Media",
                                tint = IosBlue
                            )
                        }

                        // Text Field
                        TextField(
                            value = messageInput,
                            onValueChange = {
                                messageInput = it
                                onTypingChange(it.isNotBlank())
                            },
                            placeholder = { Text("Message", color = IosTextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = IosCardBg,
                                unfocusedContainerColor = IosCardBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = IosTextPrimary,
                                unfocusedTextColor = IosTextPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("chat_text_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (messageInput.isNotBlank()) {
                            // Send text
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IosBlue)
                                    .clickable {
                                        onSendMessage(messageInput)
                                        messageInput = ""
                                        onTypingChange(false)
                                    }
                                    .testTag("send_message_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Voice recording toggle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) IosRed else IosSurface)
                                    .clickable {
                                        if (isRecording) {
                                            val result = voicePlayer.stopRecording()
                                            if (result != null) {
                                                onSendVoiceNote(result.first, result.second)
                                            }
                                        } else {
                                            voicePlayer.startRecording()
                                        }
                                    }
                                    .testTag("voice_note_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Record Voice Note",
                                    tint = if (isRecording) Color.White else IosBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Message options dialog (Delete for me / Delete for everyone)
    selectedMessageForOptions?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = { Text("Message Options", color = IosTextPrimary) },
            text = { Text("Choose an action for this message", color = IosTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(item.message.id, true)
                        selectedMessageForOptions = null
                    }
                ) {
                    Text("Delete for Everyone", color = IosRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(item.message.id, false)
                        selectedMessageForOptions = null
                    }
                ) {
                    Text("Delete for Me", color = IosTextPrimary)
                }
            },
            containerColor = IosCardBg
        )
    }
}

@Composable
fun MessageBubble(
    item: DecryptedMessage,
    isPlaying: Boolean,
    playProgress: Float,
    onPlayAudio: () -> Unit,
    onLongClick: () -> Unit
) {
    val isFromMe = item.isFromMe
    val timeFormatted = item.message.createdAt?.toDate()?.let {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        fmt.format(it)
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongClick() },
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isFromMe) 18.dp else 4.dp,
                        bottomEnd = if (isFromMe) 4.dp else 18.dp
                    )
                )
                .background(if (isFromMe) IosBubbleSent else IosBubbleReceived)
                .padding(12.dp)
        ) {
            when (item.message.mediaType) {
                "audio" -> {
                    // Voice note waveform & player
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { playProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voice Note (${item.message.audioDuration}s)",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                "image" -> {
                    // Inline image
                    Column {
                        if (item.message.mediaUrl.isNotBlank()) {
                            val imageBytes = remember(item.message.mediaUrl) {
                                try {
                                    Base64.decode(item.message.mediaUrl, Base64.NO_WRAP)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (imageBytes != null) {
                                AsyncImage(
                                    model = imageBytes,
                                    contentDescription = "Encrypted Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (item.plainText.isNotBlank() && !item.plainText.startsWith("[")) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.plainText,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                else -> {
                    // Text message
                    Text(
                        text = item.plainText,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Timestamp and delivery receipt
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IosTextSecondary,
                    fontSize = 10.sp
                )
            )

            if (isFromMe) {
                Spacer(modifier = Modifier.width(4.dp))
                when (item.message.status) {
                    "read" -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = IosBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    "delivered" -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = IosTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = IosTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
