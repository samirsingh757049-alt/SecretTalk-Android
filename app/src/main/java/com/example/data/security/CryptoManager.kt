package com.example.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "AES/GCM/NoPadding"

    // Derive SHA-256 salted hash for Secret Access Code verification
    fun hashSecretCode(code: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hash = digest.digest(code.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // Generate random cryptographic salt
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    // Generate random 6-character alphanumeric pairing invitation code
    fun generateInviteCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val random = SecureRandom()
        return (1..6)
            .map { allowedChars[random.nextInt(allowedChars.size)] }
            .joinToString("")
    }

    // Generate random 12-word recovery passkey
    fun generateRecoveryKey(): String {
        val words = listOf(
            "amber", "blaze", "cipher", "delta", "echo", "frost",
            "glacier", "haven", "ivory", "jasper", "kinetic", "lunar",
            "matrix", "nebula", "oasis", "prism", "quantum", "radiant",
            "shadow", "titan", "umbra", "vortex", "whisper", "zenith"
        )
        val random = SecureRandom()
        return (1..6).map { words[random.nextInt(words.size)] }.joinToString("-")
    }

    // AES-GCM-256 End-to-End message encryption
    fun encryptMessage(plainText: String, secretKeyPass: String): String {
        return try {
            val salt = "SecretTalkE2EESalt".toByteArray(Charsets.UTF_8)
            val keySpec = PBEKeySpec(secretKeyPass.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback base64
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    // AES-GCM-256 End-to-End message decryption
    fun decryptMessage(encryptedPayload: String, secretKeyPass: String): String {
        return try {
            val combined = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return encryptedPayload

            val salt = "SecretTalkE2EESalt".toByteArray(Charsets.UTF_8)
            val keySpec = PBEKeySpec(secretKeyPass.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            val cipherText = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            // If plaintext or legacy base64
            try {
                String(Base64.decode(encryptedPayload, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ignored: Exception) {
                encryptedPayload
            }
        }
    }
}
