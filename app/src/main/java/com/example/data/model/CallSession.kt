package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val callType: String = "audio", // "audio", "video"
    val status: String = "ringing", // "ringing", "accepted", "rejected", "ended", "busy"
    val offerSdp: String = "",
    val answerSdp: String = "",
    val callerCandidates: String = "",
    val receiverCandidates: String = "",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val endedAt: Timestamp? = null
)
