package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretTalkRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUser_cannotAccessPairing() = runBlocking {
        auth.signOut()
        val repo = SecretTalkRepository(firestore)
        val pairing = repo.getPairing()
        // Unauthenticated access should return null and fail gracefully via handleFirestoreError
        assertEquals(null, pairing)
    }

    @Test
    fun ownerUser_canRegisterOwner_andSecondUserCanPair() = runBlocking {
        // 1. Sign in User A
        val userAUid = signInTestUser("usera@secrettalk.local")
        val repoA = SecretTalkRepository(firestore)

        val regResult = repoA.registerOwner("Alice", "123456")
        assertTrue("Owner registration should succeed", regResult.isSuccess)
        val inviteCode = regResult.getOrThrow()
        assertTrue("Invite code should be 6 characters", inviteCode.length == 6)

        // Verify Secret Access Code verification works for User A
        val verifiedA = repoA.verifySecretCode("123456")
        assertTrue("Secret code 123456 should be verified", verifiedA)
        assertFalse("Wrong secret code should fail verification", repoA.verifySecretCode("999999"))

        // 2. Sign in User B
        val userBUid = signInTestUser("userb@secrettalk.local")
        val repoB = SecretTalkRepository(firestore)

        val pairResult = repoB.pairWithInviteCode(inviteCode, "Bob", "654321")
        assertTrue("User B pairing should succeed", pairResult.isSuccess)

        val pairing = repoB.getPairing()
        assertNotNull(pairing)
        assertEquals(2L, pairing?.registeredCount)
        assertTrue(pairing?.isLocked == true)

        // 3. User C (attempting 3rd registration) is rejected
        val userCUid = signInTestUser("userc@secrettalk.local")
        val repoC = SecretTalkRepository(firestore)

        val rejectedResult = repoC.pairWithInviteCode(inviteCode, "Charlie", "111111")
        assertTrue("Third user registration must be rejected", rejectedResult.isFailure)
    }

    @Test
    fun e2eeCrypto_encryptionAndDecryption() {
        val originalText = "Meet me at the secret rendezvous point."
        val secretCode = "789123"

        val cipherText = CryptoManager.encryptMessage(originalText, secretCode)
        assertFalse(cipherText == originalText)

        val decryptedText = CryptoManager.decryptMessage(cipherText, secretCode)
        assertEquals(originalText, decryptedText)

        // Decrypting with wrong key
        val wrongDecrypted = CryptoManager.decryptMessage(cipherText, "000000")
        assertFalse(wrongDecrypted == originalText)
    }
}
