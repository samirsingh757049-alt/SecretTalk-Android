package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val encryptedText: String = "",
    val mediaType: String = "text", // "text", "image", "video", "audio"
    val mediaUrl: String = "",
    val audioDuration: Long = 0L,
    val status: String = "sent", // "sent", "delivered", "read"
    val isDeleted: Boolean = false,
    val deletedFor: String = "", // user ID if deleted for only one user
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
