package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.SystemPairing
import com.example.data.model.UserProfile
import com.example.data.security.CryptoManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SecretTalkRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // --- System Pairing & Two-Account Enforcement ---

    fun observePairing(): Flow<SystemPairing?> {
        val path = "system_config/pairing"
        return db.collection("system_config").document("pairing")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(SystemPairing::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun getPairing(): SystemPairing? {
        val path = "system_config/pairing"
        return try {
            val snapshot = db.collection("system_config").document("pairing").get().await()
            if (snapshot.exists()) snapshot.toObject(SystemPairing::class.java) else null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    // Owner creates pairing & profile
    suspend fun registerOwner(displayName: String, secretCode: String): Result<String> {
        val uid = requireUserId()
        val pairingRef = db.collection("system_config").document("pairing")
        val userRef = db.collection("users").document(uid)

        return try {
            val salt = CryptoManager.generateSalt()
            val secretHash = CryptoManager.hashSecretCode(secretCode, salt)
            val inviteCode = CryptoManager.generateInviteCode()

            db.runTransaction { transaction ->
                val pairingDoc = transaction.get(pairingRef)
                if (pairingDoc.exists()) {
                    val currentOwner = pairingDoc.getString("userAId")
                    if (currentOwner != uid) {
                        throw IllegalStateException("Owner account is already registered for this system.")
                    }
                }

                val pairingData = hashMapOf(
                    "userAId" to uid,
                    "registeredCount" to 1L,
                    "isLocked" to false,
                    "inviteCode" to inviteCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(pairingRef, pairingData)

                val userData = hashMapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "secretCodeHash" to secretHash,
                    "salt" to salt,
                    "role" to "owner",
                    "isOnline" to true,
                    "isTyping" to false,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(userRef, userData)
            }.await()
            Result.success(inviteCode)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, pairingRef.path)
            Result.failure(e)
        }
    }

    // Second user pairs via invite code
    suspend fun pairWithInviteCode(inviteCode: String, displayName: String, secretCode: String): Result<Unit> {
        val uid = requireUserId()
        val pairingRef = db.collection("system_config").document("pairing")
        val userRef = db.collection("users").document(uid)

        return try {
            val salt = CryptoManager.generateSalt()
            val secretHash = CryptoManager.hashSecretCode(secretCode, salt)

            db.runTransaction { transaction ->
                val pairingDoc = transaction.get(pairingRef)
                if (!pairingDoc.exists()) {
                    throw IllegalStateException("System pairing not found. User A must register first.")
                }

                val isLocked = pairingDoc.getBoolean("isLocked") ?: false
                val registeredCount = pairingDoc.getLong("registeredCount") ?: 1L
                val userAId = pairingDoc.getString("userAId") ?: ""
                val existingInvite = pairingDoc.getString("inviteCode") ?: ""

                if (userAId == uid) {
                    throw IllegalStateException("You are already registered as User A.")
                }

                if (isLocked || registeredCount >= 2L) {
                    throw IllegalStateException("Registration is closed: Exactly two users allowed.")
                }

                if (!existingInvite.equals(inviteCode.trim(), ignoreCase = true)) {
                    throw IllegalArgumentException("Invalid invite code. Please check with User A.")
                }

                // Atomic lock and register User B
                val updateData = hashMapOf<String, Any>(
                    "userBId" to uid,
                    "registeredCount" to 2L,
                    "isLocked" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.update(pairingRef, updateData)

                val userData = hashMapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "secretCodeHash" to secretHash,
                    "salt" to salt,
                    "role" to "member",
                    "isOnline" to true,
                    "isTyping" to false,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(userRef, userData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, pairingRef.path)
            Result.failure(e)
        }
    }

    // Verify entered Secret Access Code against hashed credentials
    suspend fun verifySecretCode(code: String): Boolean {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val doc = userRef.get().await()
            if (!doc.exists()) return false
            val salt = doc.getString("salt") ?: return false
            val expectedHash = doc.getString("secretCodeHash") ?: return false
            val calculatedHash = CryptoManager.hashSecretCode(code, salt)
            calculatedHash == expectedHash
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path)
            false
        }
    }

    suspend fun updateSecretCode(newCode: String): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val salt = CryptoManager.generateSalt()
            val newHash = CryptoManager.hashSecretCode(newCode, salt)
            userRef.update(
                mapOf(
                    "secretCodeHash" to newHash,
                    "salt" to salt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    // --- User Presence & Profiles ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun updatePresence(isOnline: Boolean, isTyping: Boolean = false) {
        val uid = getCurrentUserId() ?: return
        val userRef = db.collection("users").document(uid)
        try {
            userRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "isTyping" to isTyping,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
        }
    }

    // --- Encrypted Private Chat ---

    fun observeMessages(): Flow<List<ChatMessage>> {
        val uid = requireUserId()
        val path = "messages"
        return callbackFlow {
            // Realtime listener filtering in app for 1-to-1 two-person channel
            val listener = db.collection(path)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                            .filter { msg ->
                                // Filter out messages deleted for me
                                (msg.senderId == uid || msg.receiverId == uid) &&
                                !(msg.isDeleted || msg.deletedFor == uid)
                            }
                        trySend(messages)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun sendMessage(
        receiverId: String,
        plainText: String,
        secretKey: String,
        mediaType: String = "text",
        mediaUrl: String = "",
        audioDuration: Long = 0L
    ): Result<Unit> {
        val uid = requireUserId()
        val messagesRef = db.collection("messages")
        return try {
            val encrypted = CryptoManager.encryptMessage(plainText, secretKey)
            val payload = hashMapOf(
                "senderId" to uid,
                "receiverId" to receiverId,
                "encryptedText" to encrypted,
                "mediaType" to mediaType,
                "mediaUrl" to mediaUrl,
                "audioDuration" to audioDuration,
                "status" to "sent",
                "isDeleted" to false,
                "deletedFor" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            messagesRef.add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messagesRef.path)
            Result.failure(e)
        }
    }

    suspend fun markMessageRead(messageId: String) {
        val uid = requireUserId()
        val docRef = db.collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "status" to "read",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun deleteMessage(messageId: String, forEveryone: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("messages").document(messageId)
        try {
            if (forEveryone) {
                docRef.update(
                    mapOf(
                        "isDeleted" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "deletedFor" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // --- WebRTC Audio & Video Call Signaling ---

    fun observeActiveCalls(): Flow<List<CallSession>> {
        val uid = requireUserId()
        val path = "calls"
        return callbackFlow {
            val listener = db.collection(path)
                .whereIn("status", listOf("ringing", "accepted"))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val calls = snapshot.documents.mapNotNull { it.toObject(CallSession::class.java) }
                            .filter { it.callerId == uid || it.receiverId == uid }
                        trySend(calls)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun observeCall(callId: String): Flow<CallSession?> {
        val path = "calls/$callId"
        return db.collection("calls").document(callId)
            .snapshots()
            .map { it.toObject(CallSession::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun initiateCall(receiverId: String, callType: String, offerSdp: String): Result<String> {
        val uid = requireUserId()
        val callsRef = db.collection("calls")
        return try {
            val payload = hashMapOf(
                "callerId" to uid,
                "receiverId" to receiverId,
                "callType" to callType,
                "status" to "ringing",
                "offerSdp" to offerSdp,
                "answerSdp" to "",
                "callerCandidates" to "",
                "receiverCandidates" to "",
                "durationSeconds" to 0L,
                "startedAt" to FieldValue.serverTimestamp()
            )
            val doc = callsRef.add(payload).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callsRef.path)
            Result.failure(e)
        }
    }

    suspend fun answerCall(callId: String, answerSdp: String): Result<Unit> {
        val docRef = db.collection("calls").document(callId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "accepted",
                    "answerSdp" to answerSdp
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun endCall(callId: String, durationSeconds: Long = 0L) {
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to "ended",
                    "durationSeconds" to durationSeconds,
                    "endedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun appendIceCandidate(callId: String, isCaller: Boolean, candidate: String) {
        val docRef = db.collection("calls").document(callId)
        try {
            val field = if (isCaller) "callerCandidates" else "receiverCandidates"
            docRef.update(field, candidate).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }
}
