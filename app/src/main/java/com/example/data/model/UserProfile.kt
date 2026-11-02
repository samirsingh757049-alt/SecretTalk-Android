package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val secretCodeHash: String = "",
    val salt: String = "",
    val role: String = "owner", // "owner" or "member"
    val publicKey: String = "",
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
